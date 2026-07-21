package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class TicketCategoryResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer availableQuantity;

    public TicketCategoryResponse(TicketCategory entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.price = entity.getPrice();
        this.availableQuantity = entity.getAvailableQuantity();
    }
}