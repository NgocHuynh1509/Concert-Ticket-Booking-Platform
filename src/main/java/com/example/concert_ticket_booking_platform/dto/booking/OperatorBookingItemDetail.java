package com.example.concert_ticket_booking_platform.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OperatorBookingItemDetail {
    private Long ticketCategoryId;
    private String ticketCategoryName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    private Long concertId;
    private String concertName;
    private String concertVenue;
    private LocalDateTime concertEventDate;
}