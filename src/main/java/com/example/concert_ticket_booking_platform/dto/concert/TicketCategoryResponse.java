package com.example.concert_ticket_booking_platform.dto.concert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class TicketCategoryResponse {
    private Long id;
    private Long concertId;
    private String concertName;
    private String name;
    private BigDecimal price;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer soldCount;
}