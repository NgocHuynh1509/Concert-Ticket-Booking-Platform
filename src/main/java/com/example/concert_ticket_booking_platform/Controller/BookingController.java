package com.example.concert_ticket_booking_platform.Controller;

import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import com.example.concert_ticket_booking_platform.Service.BookingService;
import com.example.concert_ticket_booking_platform.dto.booking.BookingResponse;
import com.example.concert_ticket_booking_platform.dto.booking.BookingSummaryResponse;
import com.example.concert_ticket_booking_platform.dto.booking.CreateBookingRequest;
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

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BookingResponse response = bookingService.createBooking(currentUser, idempotencyKey, request);

        HttpStatus status = response.isReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getMyBookings(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String status) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bạn chưa đăng nhập."));
        }

        BookingStatus parsedStatus = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                parsedStatus = BookingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message",
                                "status không hợp lệ, chỉ chấp nhận: PENDING_PAYMENT, PAID, CANCELLED, FAILED, EXPIRED"));
            }
        }

        List<BookingSummaryResponse> response = bookingService.getMyBookings(currentUser, parsedStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bạn chưa đăng nhập."));
        }

        BookingResponse response = bookingService.getBookingDetail(currentUser, id);
        return ResponseEntity.ok(response);
    }
}