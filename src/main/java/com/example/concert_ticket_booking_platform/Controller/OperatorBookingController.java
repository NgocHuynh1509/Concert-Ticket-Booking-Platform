package com.example.concert_ticket_booking_platform.Controller;

import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Service.OperatorBookingService;
import com.example.concert_ticket_booking_platform.dto.booking.ChangeBookingStatusRequest;
import com.example.concert_ticket_booking_platform.dto.booking.OperatorBookingDetailResponse;
import com.example.concert_ticket_booking_platform.dto.booking.OperatorBookingFilterRequest;
import com.example.concert_ticket_booking_platform.dto.booking.OperatorBookingListItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ops/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
public class OperatorBookingController {

    private final OperatorBookingService operatorBookingService;

    @GetMapping
    public ResponseEntity<Page<OperatorBookingListItemResponse>> search(OperatorBookingFilterRequest filter) {
        return ResponseEntity.ok(operatorBookingService.searchBookings(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperatorBookingDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(operatorBookingService.getBookingDetail(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OperatorBookingDetailResponse> changeStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody ChangeBookingStatusRequest request) {
        return ResponseEntity.ok(operatorBookingService.changeStatus(id, currentUser, request));
    }
}