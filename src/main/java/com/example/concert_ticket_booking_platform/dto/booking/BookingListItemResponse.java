package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1 dòng trong GET /bookings — đủ để render card trong trang "Vé của tôi"
 * mà không cần gọi thêm request. Gộp sẵn tên concert đầu tiên trong booking
 * để FE khỏi phải join thủ công.
 */
@Getter
@Builder
@AllArgsConstructor
public class BookingListItemResponse {
    private Long bookingId;
    private BookingStatus status;
    private String concertName;
    private String concertPosterUrl;
    private Integer totalTicketCount;
    private BigDecimal finalAmount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
