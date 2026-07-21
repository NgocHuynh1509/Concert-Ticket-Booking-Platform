package com.example.concert_ticket_booking_platform.dto.concert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * {id, name, price, availableQuantity} dung theo dung mo ta API cho GET /concerts/:id.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCategoryResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer availableQuantity;
}