package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OperatorBookingListItemResponse {
    private Long bookingId;
    private BookingStatus status;
    private String userEmail;
    private String userFullName;
    private String concertName;
    private String ticketCategoryName;
    private int totalTicketCount;
    private BigDecimal finalAmount;
    private PaymentStatus latestPaymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}