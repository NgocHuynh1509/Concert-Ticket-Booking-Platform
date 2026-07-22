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
    private static final int MAX_VOUCHER_RETRY = 2;

    private final TicketStockService ticketStockService;
    private final VoucherRepo voucherRepository;
    private final VoucherUsageRepo voucherUsageRepository;
    private final BookingRepo bookingRepository;
    private final BookingItemRepo bookingItemRepository;
    private final PaymentRepo paymentRepository;

    // ---------------------------------------------------------------------
    // Đặt vé trực tiếp (giữ nguyên hành vi cũ: reserve stock + tạo booking trong 1 bước)
    // ---------------------------------------------------------------------

    @Transactional
    public BookingResponse createBooking(User currentUser, String idempotencyKey, CreateBookingRequest request) {

        String normalizedVoucherCode = normalizeVoucherCode(request.getVoucherCode());

        // 1) Idempotency check trước — nếu key đã xử lý rồi thì trả về Booking đã tồn tại
        Optional<Booking> existingOpt = bookingRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            return handleExistingIdempotencyKey(existingOpt.get(), request, normalizedVoucherCode);
        }

        // 2) Trừ kho vé (optimistic lock + retry — nay dùng chung TicketStockService)
        TicketCategory ticketCategory = ticketStockService.reserveStock(
                request.getTicketCategoryId(), request.getQuantity());

        // 3) Từ đây trở đi, logic giống hệt confirmHold từ HoldService — dùng chung 1 method
        try {
            return buildBookingFromReservedStock(
                    currentUser, ticketCategory, request.getQuantity(),
                    idempotencyKey, normalizedVoucherCode);
        } catch (RuntimeException e) {
            // Nếu bước tạo Booking/Voucher thất bại SAU KHI đã trừ kho ở bước 2,
            // phải hoàn lại kho — nếu không vé sẽ bị "bốc hơi" khỏi hệ thống.
            ticketStockService.releaseStock(ticketCategory.getId(), request.getQuantity());
            throw e;
        }
    }

    // ---------------------------------------------------------------------
    // Dùng chung bởi createBooking() (đặt trực tiếp) và HoldService.confirmHold()
    // (đặt từ 1 hold đã trừ kho từ trước — nên KHÔNG gọi reserveStock ở đây nữa)
    // ---------------------------------------------------------------------

    @Transactional
    public BookingResponse buildBookingFromReservedStock(
            User currentUser, TicketCategory ticketCategory, int quantity,
            String idempotencyKey, String normalizedVoucherCode) {

        Voucher voucher = null;
        BigDecimal totalAmount = ticketCategory.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (normalizedVoucherCode != null) {
            voucher = validateAndLoadVoucher(normalizedVoucherCode, currentUser);
            discountAmount = applyDiscount(voucher, totalAmount);
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
            incrementVoucherUsage(voucher);
            voucherUsageRepository.save(VoucherUsage.builder()
                    .voucher(voucher)
                    .user(currentUser)
                    .booking(savedBooking)
                    .build());
        }

        return mapToBookingResponse(savedBooking, List.of(savedItem), savedPayment, false);
    }

    // Dùng bởi HoldService khi hold đã ở trạng thái CONFIRMED (retry) — trả về booking đã có sẵn
    // theo đúng quyền sở hữu, không tạo lại / xử lý lại nghiệp vụ.
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

    // ---------------------------------------------------------------------
    // Idempotency replay
    // ---------------------------------------------------------------------

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

    // ---------------------------------------------------------------------
    // Voucher
    // ---------------------------------------------------------------------

    private Voucher validateAndLoadVoucher(String code, User user) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new VoucherInvalidException("Voucher không tồn tại"));

        if (!Boolean.TRUE.equals(voucher.getActive())) {
            throw new VoucherInvalidException("Voucher hiện không khả dụng");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getValidFrom()) || now.isAfter(voucher.getValidTo())) {
            throw new VoucherInvalidException("Voucher đã hết hạn hoặc chưa tới thời gian áp dụng");
        }

        if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
            throw new VoucherInvalidException("Voucher đã hết lượt sử dụng");
        }

        long usedByThisUser = voucherUsageRepository.countByVoucherAndUser(voucher, user);
        if (usedByThisUser >= voucher.getPerUserLimit()) {
            throw new VoucherInvalidException("Bạn đã dùng hết số lần cho phép của voucher này");
        }

        return voucher;
    }

    private BigDecimal applyDiscount(Voucher voucher, BigDecimal totalAmount) {
        BigDecimal discount = switch (voucher.getDiscountType()) {
            case PERCENTAGE -> totalAmount
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> voucher.getDiscountValue();
        };
        return discount.min(totalAmount);
    }

    private void incrementVoucherUsage(Voucher voucher) {
        int attempt = 0;
        while (true) {
            attempt++;
            if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
                throw new VoucherInvalidException("Voucher đã hết lượt sử dụng");
            }
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            try {
                voucherRepository.saveAndFlush(voucher);
                return;
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                if (attempt > MAX_VOUCHER_RETRY) {
                    throw new VoucherInvalidException("Voucher vừa hết lượt, vui lòng thử lại");
                }
                voucher = voucherRepository.findById(voucher.getId())
                        .orElseThrow(() -> new VoucherInvalidException("Voucher không tồn tại"));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Mapper Helper
    // ---------------------------------------------------------------------

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

    /**
     * GET /bookings — danh sách booking của user hiện tại.
     * statusFilter == null -> tab "Tất cả", không lọc.
     */
    @Transactional(readOnly = true)
    public List<BookingListItemResponse> getMyBookings(User currentUser, BookingStatus statusFilter) {
        List<Booking> bookings = (statusFilter == null)
                ? bookingRepository.findByUser_IdOrderByCreatedAtDesc(currentUser.getId())
                : bookingRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(currentUser.getId(), statusFilter);

        return bookings.stream().map(this::toListItem).toList();
    }

    /**
     * GET /bookings/:id — chi tiết 1 booking.
     * 404 nếu booking không tồn tại, 403 nếu không phải chủ booking (theo đúng đặc tả API).
     */
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(User currentUser, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking id=" + bookingId));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bạn không có quyền xem booking này");
        }

        return toDetail(booking);
    }

    private BookingListItemResponse toListItem(Booking booking) {
        // Hiện tại 1 booking chỉ chứa vé của đúng 1 concert (POST /bookings chỉ nhận
        // 1 ticketCategoryId/lần), nên lấy concert đại diện từ item đầu tiên là an toàn.
        BookingItem firstItem = booking.getItems().isEmpty() ? null : booking.getItems().get(0);
        TicketCategory tc = firstItem != null ? firstItem.getTicketCategory() : null;

        int totalTickets = booking.getItems().stream()
                .mapToInt(BookingItem::getQuantity)
                .sum();

        return BookingListItemResponse.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .concertName(tc != null ? tc.getConcert().getName() : null)
                .concertPosterUrl(tc != null ? tc.getConcert().getPosterUrl() : null)
                .totalTicketCount(totalTickets)
                .finalAmount(booking.getFinalAmount())
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private BookingDetailResponse toDetail(Booking booking) {
        BookingItem firstItem = booking.getItems().isEmpty() ? null : booking.getItems().get(0);
        TicketCategory tc = firstItem != null ? firstItem.getTicketCategory() : null;

        List<BookingItemResponse> items = booking.getItems().stream()
                .map(it -> BookingItemResponse.builder()
                        .ticketCategoryId(it.getTicketCategory().getId())
                        .ticketCategoryName(it.getTicketCategory().getName())
                        .quantity(it.getQuantity())
                        .unitPrice(it.getUnitPrice())
                        .subtotal(it.getSubtotal())
                        .build())
                .toList();

        return BookingDetailResponse.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .concertName(tc != null ? tc.getConcert().getName() : null)
                .concertVenue(tc != null ? tc.getConcert().getVenue() : null)
                .concertEventDate(tc != null ? tc.getConcert().getEventDate() : null)
                .concertPosterUrl(tc != null ? tc.getConcert().getPosterUrl() : null)
                .voucherCode(booking.getVoucher() != null ? booking.getVoucher().getCode() : null)
                .totalAmount(booking.getTotalAmount())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .items(items)
                .build();
    }

}
