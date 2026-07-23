package com.example.concert_ticket_booking_platform.Controller;

import com.example.concert_ticket_booking_platform.Service.ConcertOpsService;
import com.example.concert_ticket_booking_platform.Service.TicketCategoryService;
import com.example.concert_ticket_booking_platform.dto.concert.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ops/concerts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
public class OperatorConcertController {

    private final ConcertOpsService concertOpsService;

    @GetMapping
    public ResponseEntity<List<ConcertSummaryResponse>> listAllConcerts() {
        return ResponseEntity.ok(concertOpsService.listAllConcerts());
    }


    @PostMapping
    public ResponseEntity<ConcertOpsResponse> createConcert(@Valid @RequestBody CreateConcertOpsRequest request) {
        ConcertOpsResponse response = concertOpsService.createConcert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PatchMapping("/{id}/status")
    public ResponseEntity<ConcertOpsResponse> updateStatus(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateConcertStatusRequest request) {
        ConcertOpsResponse response = concertOpsService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ticket-categories")
    public ResponseEntity<List<TicketCategoryResponse>> getTicketCategories(
            @RequestParam(required = false) Long concertId,
            @RequestParam(required = false) String name
    ) {
        return ResponseEntity.ok(concertOpsService.getTicketCategories(concertId, name));
    }
}
