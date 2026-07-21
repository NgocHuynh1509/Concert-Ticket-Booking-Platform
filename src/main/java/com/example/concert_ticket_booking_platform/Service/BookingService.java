package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.*;
import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentMethod;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentStatus;
import com.example.concert_ticket_booking_platform.Repository.*;
import com.example.concert_ticket_booking_platform.dto.booking.BookingItemResponse;
import com.example.concert_ticket_booking_platform.dto.booking.BookingResponse;
import com.example.concert_ticket_booking_platform.dto.booking.CreateBookingRequest;
import com.example.concert_ticket_booking_platform.dto.booking.PaymentResponse;
import com.example.concert_ticket_booking_platform.exception.booking.IdempotencyConflictException;
import com.example.concert_ticket_booking_platform.exception.booking.InsufficientTicketException;
import com.example.concert_ticket_booking_platform.exception.booking.VoucherInvalidException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final int MAX_STOCK_RETRY = 2; // retry thêm tối đa 2 lần khi optimistic lock conflict

    private final TicketCategoryRepo ticketCategoryRepository;
    private final VoucherRepo voucherRepository;
    private final VoucherUsageRepo voucherUsageRepository;
    private final BookingRepo bookingRepository;
    private final BookingItemRepo bookingItemRepository;
    private final PaymentRepo paymentRepository;
    private final EntityManager entityManager;

    @Transactional
    public BookingResponse createBooking(User currentUser, String idempotencyKey, CreateBookingRequest request) {

        String normalizedVoucherCode = normalizeVoucherCode(request.getVoucherCode());

        // 1) Idempotency check trước — nếu key đã xử lý rồi thì trả về Booking đã tồn tại
        Optional<Booking> existingOpt = bookingRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            return handleExistingIdempotencyKey(existingOpt.get(), request, normalizedVoucherCode);
        }

        // 2) Trừ kho vé (optimistic lock + retry)
        TicketCategory ticketCategory = reserveStock(request.getTicketCategoryId(), request.getQuantity());

        // 3) Áp voucher (nếu có)
        Voucher voucher = null;
        BigDecimal totalAmount = ticketCategory.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (normalizedVoucherCode != null) {
            voucher = validateAndLoadVoucher(normalizedVoucherCode, currentUser);
            discountAmount = applyDiscount(voucher, totalAmount);
        }

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        // 4) Lưu Booking vào DB
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
            return handleExistingIdempotencyKey(winner, request, normalizedVoucherCode);
        }

        // 5) Lưu BookingItem đàng hoàng vào DB qua bookingItemRepository
        BookingItem item = BookingItem.builder()
                .booking(savedBooking)
                .ticketCategory(ticketCategory)
                .quantity(request.getQuantity())
                .unitPrice(ticketCategory.getPrice())
                .subtotal(totalAmount)
                .build();

        BookingItem savedItem = bookingItemRepository.save(item);

        // 6) Lưu Payment đàng hoàng vào DB qua paymentRepository
        Payment initialPayment = Payment.builder()
                .booking(savedBooking)
                .amount(finalAmount)
                .method(PaymentMethod.BANK_TRANSFER) // Default phương thức giữ chỗ
                .status(PaymentStatus.PENDING)
                .transactionRef("TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        Payment savedPayment = paymentRepository.save(initialPayment);

        // 7) Ghi nhận lượt dùng voucher
        if (voucher != null) {
            incrementVoucherUsage(voucher);
            voucherUsageRepository.save(VoucherUsage.builder()
                    .voucher(voucher)
                    .user(currentUser)
                    .booking(savedBooking)
                    .build());
        }

        // 8) Map sang BookingResponse DTO
        return mapToBookingResponse(savedBooking, List.of(savedItem), savedPayment, false);
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
    // Trừ kho vé — optimistic lock (@Version) + retry khi conflict
    // ---------------------------------------------------------------------

    private TicketCategory reserveStock(Long ticketCategoryId, int quantity) {
        int attempt = 0;
        while (true) {
            attempt++;
            TicketCategory ticketCategory = ticketCategoryRepository.findById(ticketCategoryId)
                    .orElseThrow(() -> new InsufficientTicketException("Loại vé không tồn tại"));

            if (ticketCategory.getAvailableQuantity() < quantity) {
                throw new InsufficientTicketException("Vé đã hết hoặc không đủ số lượng yêu cầu");
            }
            ticketCategory.setAvailableQuantity(ticketCategory.getAvailableQuantity() - quantity);

            try {
                return ticketCategoryRepository.saveAndFlush(ticketCategory);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                entityManager.clear();
                if (attempt > MAX_STOCK_RETRY) {
                    throw new InsufficientTicketException(
                            "Vé vừa được người khác đặt, vui lòng thử lại");
                }
            }
        }
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
                entityManager.clear();
                if (attempt > MAX_STOCK_RETRY) {
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
}