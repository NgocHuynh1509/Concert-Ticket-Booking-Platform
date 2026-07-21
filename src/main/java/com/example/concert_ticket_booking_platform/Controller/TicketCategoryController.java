package com.example.concert_ticket_booking_platform.Controller;

import com.example.concert_ticket_booking_platform.Repository.TicketCategoryRepo;
import com.example.concert_ticket_booking_platform.dto.booking.TicketCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ticket-categories")
@RequiredArgsConstructor
public class TicketCategoryController {

    private final TicketCategoryRepo ticketCategoryRepo;

    @GetMapping("/{id}")
    public ResponseEntity<?> getTicketCategoryInfo(@PathVariable Long id) {
        var ticketCategoryOpt = ticketCategoryRepo.findById(id);

        if (ticketCategoryOpt.isPresent()) {
            // Map từ Entity sang DTO an toàn không lo dính vòng lặp JSON
            return ResponseEntity.ok(new TicketCategoryResponse(ticketCategoryOpt.get()));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Loại vé không tồn tại"));
    }
}