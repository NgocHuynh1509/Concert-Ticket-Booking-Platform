package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.*;
import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentMethod;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentStatus;
import com.example.concert_ticket_booking_platform.Repository.*;
import com.example.concert_ticket_booking_platform.dto.booking.*;
import com.example.concert_ticket_booking_platform.exception.ResourceNotFoundException;
import com.example.concert_ticket_booking_platform.exception.booking.ForbiddenException;
import com.example.concert_ticket_booking_platform.exception.booking.IdempotencyConflictException;
import com.example.concert_ticket_booking_platform.exception.booking.VoucherInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.OptimisticLockException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int BOOKING_HOLD_MINUTES = 15;
    private final TicketStockService ticketStockService;
    private final VoucherService voucherService;
    private final BookingRepo bookingRepository;
    private final BookingItemRepo bookingItemRepository;
    private final PaymentRepo paymentRepository;

    @Transactional
    public BookingResponse createBooking(User currentUser, String idempotencyKey, CreateBookingRequest request) {

        String normalizedVoucherCode = normalizeVoucherCode(request.getVoucherCode());

        Optional<Booking> existingOpt = bookingRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            return handleExistingIdempotencyKey(existingOpt.get(), request, normalizedVoucherCode);
        }

        TicketCategory ticketCategory = ticketStockService.reserveStock(
                request.getTicketCategoryId(), request.getQuantity());

        try {
            return buildBookingFromReservedStock(
                    currentUser, ticketCategory, request.getQuantity(),
                    idempotencyKey, normalizedVoucherCode);
        } catch (RuntimeException e) {
            ticketStockService.releaseStock(ticketCategory.getId(), request.getQuantity());
            throw e;
        }
    }

    @Transactional
    public BookingResponse buildBookingFromReservedStock(
            User currentUser, TicketCategory ticketCategory, int quantity,
            String idempotencyKey, String normalizedVoucherCode) {

        Voucher voucher = null;
        BigDecimal totalAmount = ticketCategory.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (normalizedVoucherCode != null) {
            voucher = voucherService.validateAndLoadVoucher(normalizedVoucherCode, currentUser);
            discountAmount = voucherService.applyDiscount(voucher, totalAmount);
        }

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        Booking booking = Booking.builder()
                .user(currentUser)
                .voucher(voucher)
                .status(BookingStatus.PENDING_PAYMENT)
                .idempotencyKey(idempotencyKey)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .expiresAt(LocalDateTime.now().plusMinutes(BOOKING_HOLD_MINUTES))
                .build();

        Booking savedBooking;
        try {
            savedBooking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            Booking winner = bookingRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e);
            List<BookingItem> items = bookingItemRepository.findByBookingId(winner.getId());
            Payment payment = paymentRepository.findByBookingId(winner.getId()).orElse(null);
            return mapToBookingResponse(winner, items, payment, true);
        }

        BookingItem item = BookingItem.builder()
                .booking(savedBooking)
                .ticketCategory(ticketCategory)
                .quantity(quantity)
                .unitPrice(ticketCategory.getPrice())
                .subtotal(totalAmount)
                .build();
        BookingItem savedItem = bookingItemRepository.save(item);

        Payment initialPayment = Payment.builder()
                .booking(savedBooking)
                .amount(finalAmount)
                .method(PaymentMethod.BANK_TRANSFER) // Default phương thức giữ chỗ
                .status(PaymentStatus.PENDING)
                .transactionRef("TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
        Payment savedPayment = paymentRepository.save(initialPayment);

        if (voucher != null) {
            voucher = voucherService.incrementVoucherUsage(voucher);
            voucherService.recordVoucherUsage(voucher, currentUser, savedBooking);
        }

        return mapToBookingResponse(savedBooking, List.of(savedItem), savedPayment, false);
    }


    public BookingResponse getBookingResponse(Long bookingId, User currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking không tồn tại: " + bookingId));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new IdempotencyConflictException("Booking này không thuộc về bạn");
        }

        List<BookingItem> items = bookingItemRepository.findByBookingId(booking.getId());
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        return mapToBookingResponse(booking, items, payment, true);
    }



    private BookingResponse handleExistingIdempotencyKey(Booking existing, CreateBookingRequest request, String normalizedVoucherCode) {
        boolean samePayload = isSamePayload(existing, request, normalizedVoucherCode);
        if (!samePayload) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key đã được dùng cho một request khác với dữ liệu khác");
        }

        List<BookingItem> items = bookingItemRepository.findByBookingId(existing.getId());
        Payment payment = paymentRepository.findByBookingId(existing.getId()).orElse(null);

        return mapToBookingResponse(existing, items, payment, true);
    }

    private boolean isSamePayload(Booking existing, CreateBookingRequest request, String normalizedVoucherCode) {
        List<BookingItem> items = bookingItemRepository.findByBookingId(existing.getId());
        if (items.isEmpty()) {
            return false;
        }
        BookingItem item = items.get(0);
        boolean sameTicket = Objects.equals(item.getTicketCategory().getId(), request.getTicketCategoryId());
        boolean sameQuantity = Objects.equals(item.getQuantity(), request.getQuantity());
        String existingVoucherCode = existing.getVoucher() != null ? existing.getVoucher().getCode() : null;
        boolean sameVoucher = Objects.equals(existingVoucherCode, normalizedVoucherCode);
        return sameTicket && sameQuantity && sameVoucher;
    }

    private String normalizeVoucherCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }





    private BookingResponse mapToBookingResponse(Booking booking, List<BookingItem> items, Payment payment, boolean isReplay) {
        List<BookingItemResponse> itemDtos = items != null ? items.stream().map(item -> BookingItemResponse.builder()
                .id(item.getId())
                .ticketCategoryId(item.getTicketCategory().getId())
                .ticketCategoryName(item.getTicketCategory().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build()).toList() : new ArrayList<>();

        PaymentResponse paymentDto = payment != null ? PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionRef(payment.getTransactionRef())
                .paidAt(payment.getPaidAt())
                .build() : null;

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser() != null ? booking.getUser().getId() : null)
                .voucherCode(booking.getVoucher() != null ? booking.getVoucher().getCode() : null)
                .status(booking.getStatus())
                .idempotencyKey(booking.getIdempotencyKey())
                .totalAmount(booking.getTotalAmount())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .items(itemDtos)
                .payment(paymentDto)
                .isReplay(isReplay)
                .build();
    }


    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getMyBookings(User currentUser, BookingStatus statusFilter) {
        List<Booking> bookings = (statusFilter == null)
                ? bookingRepository.findByUser_IdOrderByCreatedAtDesc(currentUser.getId())
                : bookingRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(currentUser.getId(), statusFilter);

        return bookings.stream().map(this::toSummary).toList();
    }


    @Transactional(readOnly = true)
    public BookingResponse getBookingDetail(User currentUser, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking id=" + bookingId));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bạn không có quyền xem booking này");
        }

        List<BookingItem> items = bookingItemRepository.findByBookingId(booking.getId());
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        return mapToBookingResponse(booking, items, payment, false);
    }

    private BookingSummaryResponse toSummary(Booking booking) {
        var items = bookingItemRepository.findByBookingId(booking.getId());

        BookingItem firstItem = items.isEmpty() ? null : items.get(0);
        TicketCategory tc = firstItem != null ? firstItem.getTicketCategory() : null;

        int ticketCount = items.stream().mapToInt(BookingItem::getQuantity).sum();

        return BookingSummaryResponse.builder()
                .id(booking.getId())
                .status(booking.getStatus())
                .concertName(tc != null ? tc.getConcert().getName() : null)
                .concertPosterUrl(tc != null ? tc.getConcert().getPosterUrl() : null)
                .ticketCount(ticketCount)
                .finalAmount(booking.getFinalAmount())
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
