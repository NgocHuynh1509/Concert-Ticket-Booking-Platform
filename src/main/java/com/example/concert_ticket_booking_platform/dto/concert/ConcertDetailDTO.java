package com.example.concert_ticket_booking_platform.dto.concert;



import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConcertDetailDTO {

    private Long id;
    private String name;
    private String description;
    private String venue;
    private LocalDateTime eventDate;
    private String status;
    private String posterUrl;
    private String concertMapUrl;

    private List<TicketDTO> tickets;

    @Data
    public static class TicketDTO{
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer availableQuantity;
    }
}