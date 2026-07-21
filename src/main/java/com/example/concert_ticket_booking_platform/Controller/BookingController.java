package com.example.concert_ticket_booking_platform.Controller;

import com.example.concert_ticket_booking_platform.Entity.Booking;
import com.example.concert_ticket_booking_platform.Entity.Payment;
import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Repository.BookingItemRepo;
import com.example.concert_ticket_booking_platform.Repository.BookingRepo;
import com.example.concert_ticket_booking_platform.Repository.PaymentRepo;
import com.example.concert_ticket_booking_platform.Service.BookingService;
import com.example.concert_ticket_booking_platform.dto.booking.BookingItemResponse;
import com.example.concert_ticket_booking_platform.dto.booking.BookingResponse;
import com.example.concert_ticket_booking_platform.dto.booking.CreateBookingRequest;
import com.example.concert_ticket_booking_platform.dto.booking.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepo bookingRepository;
    private final BookingItemRepo bookingItemRepository;
    private final PaymentRepo paymentRepository;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request) {

        // 1. Kiểm tra Authentication
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Gọi Service xử lý (Tất cả Exception đã được GlobalExceptionHandler quản lý)
        BookingResponse response = bookingService.createBooking(currentUser, idempotencyKey, request);

        // 3. Nếu là Replay -> Trả về 200 OK, nếu Tạo mới -> Trả về 201 CREATED
        HttpStatus status = response.isReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bạn chưa đăng nhập."));
        }

        return bookingRepository.findById(id)
                .map(booking -> {
                    if (!booking.getUser().getId().equals(currentUser.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message", "Bạn không có quyền xem đơn hàng này."));
                    }

                    // Map Entity sang BookingResponse DTO kèm đầy đủ items & payment
                    BookingResponse response = mapToResponse(booking);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Không tìm thấy thông tin đơn hàng.")));
    }

    // --- Helper Mapper cho hàm GET detail ---
    private BookingResponse mapToResponse(Booking booking) {
        var items = bookingItemRepository.findByBookingId(booking.getId());
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);

        List<BookingItemResponse> itemDtos = items.stream().map(item -> BookingItemResponse.builder()
                .id(item.getId())
                .ticketCategoryId(item.getTicketCategory().getId())
                .ticketCategoryName(item.getTicketCategory().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build()).toList();

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
                .userId(booking.getUser().getId())
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
                .build();
    }
}