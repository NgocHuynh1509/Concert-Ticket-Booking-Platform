package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BookingSummaryResponse {
    private Long id;
    private BookingStatus status;
    private String concertName;
    private String concertPosterUrl;
    private Integer ticketCount;
    private BigDecimal finalAmount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}