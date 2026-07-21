package com.example.concert_ticket_booking_platform.Controller;

import com.example.concert_ticket_booking_platform.Service.ConcertService;
import com.example.concert_ticket_booking_platform.dto.concert.ConcertSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping
    public ResponseEntity<List<ConcertSummaryResponse>> getAllConcerts() {
        return ResponseEntity.ok(concertService.getAllConcerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getConcert(@PathVariable Long id){

        return ResponseEntity.ok(concertService.getConcertDetail(id));

    }
}
