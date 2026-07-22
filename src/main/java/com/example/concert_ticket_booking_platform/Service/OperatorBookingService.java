package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.*;
import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentStatus;
import com.example.concert_ticket_booking_platform.Repository.BookingRepo;
import com.example.concert_ticket_booking_platform.Repository.BookingSpecification;
import com.example.concert_ticket_booking_platform.Repository.BookingStatusHistoryRepo;
import com.example.concert_ticket_booking_platform.Repository.PaymentRepo;
import com.example.concert_ticket_booking_platform.dto.booking.*;
import com.example.concert_ticket_booking_platform.exception.InvalidStatusTransitionException;
import com.example.concert_ticket_booking_platform.exception.booking.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OperatorBookingService {

    private final BookingRepo bookingRepository;
    private final PaymentRepo paymentRepository; // cần thêm findAllByBookingId, xem ghi chú dưới
    private final BookingStatusHistoryRepo bookingStatusHistoryRepository;
    private final TicketStockService ticketStockService;

    @Transactional(readOnly = true)
    public Page<OperatorBookingListItemResponse> searchBookings(OperatorBookingFilterRequest filter) {
        Specification<Booking> spec = BookingSpecification.fromFilter(filter);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return bookingRepository.findAll(spec, pageable)
                .map(this::toListItem);
    }

    // Chỉ định nghĩa các bước chuyển HỢP LỆ do operator thao tác thủ công.
    // Các chuyển trạng thái tự động (PENDING_PAYMENT -> PAID do payment callback,
    // -> EXPIRED do scheduled job) không đi qua đường này.
    // CANCELLED & EXPIRED là trạng thái cuối -> Không cho phép chuyển đổi tiếp
    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS = Map.of(
            BookingStatus.PENDING_PAYMENT, Set.of(BookingStatus.PAID, BookingStatus.CANCELLED),
            BookingStatus.PAID, Set.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CANCELLED),
            BookingStatus.CANCELLED, Set.of(), // 👈 Không thể đổi
            BookingStatus.EXPIRED, Set.of()    // 👈 Không thể đổi
    );

    @Transactional
    public OperatorBookingDetailResponse changeStatus(Long bookingId, User operator, ChangeBookingStatusRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking id=" + bookingId));

        BookingStatus from = booking.getStatus();
        BookingStatus to = request.getStatus();

        if (from == to) {
            throw new InvalidStatusTransitionException("Booking đang ở trạng thái này rồi");
        }

        Set<BookingStatus> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowedNext.contains(to)) {
            throw new InvalidStatusTransitionException(
                    "Không thể chuyển từ " + from + " sang " + to);
        }

        if (to == BookingStatus.CANCELLED) {
            for (BookingItem item : booking.getItems()) {
                ticketStockService.releaseStock(item.getTicketCategory().getId(), item.getQuantity());
            }
        }

        booking.setStatus(to);
        bookingRepository.save(booking);

        bookingStatusHistoryRepository.save(BookingStatusHistory.builder()
                .booking(booking)
                .fromStatus(from)
                .toStatus(to)
                .reason(request.getReason().trim())
                .changedBy(operator)
                .build());

        return getBookingDetail(bookingId);
    }

    @Transactional(readOnly = true)
    public OperatorBookingDetailResponse getBookingDetail(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking id=" + bookingId));

        List<PaymentResponse> paymentDtos = paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream()
                .map(p -> PaymentResponse.builder()
                        .id(p.getId())
                        .amount(p.getAmount())
                        .method(p.getMethod())
                        .status(p.getStatus())
                        .transactionRef(p.getTransactionRef())
                        .paidAt(p.getPaidAt())
                        .build())
                .toList();

        List<OperatorBookingItemDetail> itemDtos = booking.getItems().stream()
                .map(it -> {
                    TicketCategory tc = it.getTicketCategory();
                    return OperatorBookingItemDetail.builder()
                            .ticketCategoryId(tc.getId())
                            .ticketCategoryName(tc.getName())
                            .quantity(it.getQuantity())
                            .unitPrice(it.getUnitPrice())
                            .subtotal(it.getSubtotal())
                            .concertId(tc.getConcert().getId())
                            .concertName(tc.getConcert().getName())
                            .concertVenue(tc.getConcert().getVenue())
                            .concertEventDate(tc.getConcert().getEventDate())
                            .build();
                }).toList();

        List<BookingStatusHistoryResponse> historyDtos = bookingStatusHistoryRepository
                .findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream()
                .map(h -> BookingStatusHistoryResponse.builder()
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .reason(h.getReason())
                        .changedByEmail(h.getChangedBy().getEmail())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();

        return OperatorBookingDetailResponse.builder()
                .userId(booking.getUser().getId())
                .userEmail(booking.getUser().getEmail())
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .idempotencyKey(booking.getIdempotencyKey())
                .totalAmount(booking.getTotalAmount())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .voucherCode(booking.getVoucher() != null ? booking.getVoucher().getCode() : null)
                .createdAt(booking.getCreatedAt())
                .expiresAt(booking.getExpiresAt())
                .items(itemDtos)
                .payments(paymentDtos)
                .statusHistory(historyDtos)
                .build();
    }

    private OperatorBookingListItemResponse toListItem(Booking booking) {
        BookingItem firstItem = booking.getItems().isEmpty() ? null : booking.getItems().get(0);
        TicketCategory tc = firstItem != null ? firstItem.getTicketCategory() : null;

        int totalTickets = booking.getItems().stream().mapToInt(BookingItem::getQuantity).sum();

        PaymentStatus latestStatus = paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(booking.getId())
                .stream().findFirst().map(Payment::getStatus).orElse(null);

        return OperatorBookingListItemResponse.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .userEmail(booking.getUser().getEmail())
                .concertName(tc != null ? tc.getConcert().getName() : null)
                .ticketCategoryName(tc != null ? tc.getName() : null)
                .totalTicketCount(totalTickets)
                .finalAmount(booking.getFinalAmount())
                .latestPaymentStatus(latestStatus)
                .createdAt(booking.getCreatedAt())
                .expiresAt(booking.getExpiresAt())
                .build();
    }
}
