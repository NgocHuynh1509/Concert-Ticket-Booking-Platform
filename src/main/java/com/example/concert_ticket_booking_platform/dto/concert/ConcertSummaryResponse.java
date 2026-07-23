package com.example.concert_ticket_booking_platform.dto.concert;

import com.example.concert_ticket_booking_platform.Entity.enums.ConcertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcertSummaryResponse {
    private Long id;
    private String name;
    private String venue;
    private LocalDateTime eventDate;
    private ConcertStatus status;
    private String posterUrl;
}