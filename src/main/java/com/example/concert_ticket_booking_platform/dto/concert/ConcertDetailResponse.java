package com.example.concert_ticket_booking_platform.dto.concert;

import com.example.concert_ticket_booking_platform.Entity.enums.ConcertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcertDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String venue;
    private LocalDateTime eventDate;
    private ConcertStatus status;
    private String concertMapUrl;
    private String posterUrl;
    private List<TicketCategoryResponse> ticketCategories;
}