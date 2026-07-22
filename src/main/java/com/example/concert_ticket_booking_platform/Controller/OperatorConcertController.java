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

/**
 * Dashboard API danh cho OPERATOR/ADMIN.
 *
 * Phan quyen 2 lop cho endpoint nay:
 * 1) SecurityConfig da chan "/api/ops/**" o muc URL-pattern (coarse-grained): 401 neu
 *    chua dang nhap, 403 neu dang nhap nhung khong phai OPERATOR/ADMIN.
 * 2) @PreAuthorize o day la lop fine-grained thu 2 (defense in depth) - neu sau nay
 *    controller nay bi mount duoi 1 path khac quen sua SecurityConfig, van khong bi ho.
 */
@RestController
@RequestMapping("/api/ops/concerts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
public class OperatorConcertController {

    private final ConcertOpsService concertOpsService;


    /**
     * GET /api/ops/concerts
     * Danh sach TAT CA concert (moi status) de operator quan ly - khac voi
     * GET /api/concerts cong khai chi tra PUBLISHED.
     */
    @GetMapping
    public ResponseEntity<List<ConcertSummaryResponse>> listAllConcerts() {
        return ResponseEntity.ok(concertOpsService.listAllConcerts());
    }

    /**
     * POST /api/ops/concerts
     * Operator tao concert moi kem cac ticket category.
     * Body: name, venue, eventDate, ticketCategories: [{name, price, totalQuantity}]
     * 200: { concertId, status: "DRAFT" }
     * 400: thieu input / sai dinh dang (bat boi @Valid -> GlobalExceptionHandler)
     * 403: khong du quyen (bat boi Spring Security -> GlobalExceptionHandler)
     */
    @PostMapping
    public ResponseEntity<ConcertOpsResponse> createConcert(@Valid @RequestBody CreateConcertOpsRequest request) {
        ConcertOpsResponse response = concertOpsService.createConcert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PATCH /api/ops/concerts/{id}/status
     * Cap nhat trang thai concert (publish/huy/hoan tat).
     * Body: { status }
     * 200: { concertId, status }
     * 400: transition khong hop le
     * 404: concert khong ton tai
     */
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
