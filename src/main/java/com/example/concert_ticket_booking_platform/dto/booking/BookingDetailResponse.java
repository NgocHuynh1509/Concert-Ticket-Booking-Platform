package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class BookingDetailResponse {
    private Long bookingId;
    private BookingStatus status;
    private String concertName;
    private String concertVenue;
    private LocalDateTime concertEventDate;
    private String concertPosterUrl;
    private String voucherCode;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private List<BookingItemResponse> items;
}
