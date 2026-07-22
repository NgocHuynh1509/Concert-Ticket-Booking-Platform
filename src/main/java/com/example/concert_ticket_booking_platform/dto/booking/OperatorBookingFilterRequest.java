package com.example.concert_ticket_booking_platform.dto.booking;



import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperatorBookingFilterRequest {
    private Long concertId;
    private Long ticketCategoryId;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private String userEmail;      // tìm gần đúng theo email
    private String idempotencyKey; // hữu ích khi debug 1 request cụ thể

    // pagination
    private int page = 0;
    private int size = 20;
}