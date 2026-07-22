package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO dùng riêng cho GET /api/bookings (danh sách "Vé của tôi").
 * Nhẹ hơn BookingResponse — không kéo theo items/payment chi tiết vì trang danh sách
 * chỉ cần đủ info để render card, tránh N+1 query không cần thiết khi list nhiều booking.
 */
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