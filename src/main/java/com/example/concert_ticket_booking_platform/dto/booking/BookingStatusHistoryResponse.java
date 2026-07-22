package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingStatusHistoryResponse {
    private BookingStatus fromStatus;
    private BookingStatus toStatus;
    private String reason;
    private String changedByEmail;
    private LocalDateTime createdAt;
}