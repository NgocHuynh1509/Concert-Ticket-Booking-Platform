package com.example.concert_ticket_booking_platform.Controller;

import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Service.HoldService;
import com.example.concert_ticket_booking_platform.dto.booking.BookingResponse;
import com.example.concert_ticket_booking_platform.dto.hold.ConfirmHoldRequest;
import com.example.concert_ticket_booking_platform.dto.hold.CreateHoldRequest;
import com.example.concert_ticket_booking_platform.dto.hold.HoldResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holds")
@RequiredArgsConstructor
public class HoldController {

    private final HoldService holdService;

    // POST /api/holds — giữ chỗ tạm, cần Idempotency-Key giống hệt convention của /api/bookings
    @PostMapping
    public ResponseEntity<HoldResponse> createHold(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateHoldRequest request) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        HoldResponse response = holdService.createHold(currentUser, idempotencyKey, request);
        HttpStatus status = response.isReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    // POST /api/holds/{id}/confirm — biến hold thành Booking + Payment thật
    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmHold(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ConfirmHoldRequest request) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ConfirmHoldRequest safeRequest = request != null ? request : new ConfirmHoldRequest();
        BookingResponse response = holdService.confirmHold(currentUser, id, safeRequest);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/holds/{id} — user chủ động huỷ hold trước khi confirm
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> releaseHold(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        holdService.releaseHold(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
