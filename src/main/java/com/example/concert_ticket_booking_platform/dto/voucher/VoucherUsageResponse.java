package com.example.concert_ticket_booking_platform.dto.voucher;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VoucherUsageResponse {
    private String username;
    private String email;
    private LocalDateTime usedAt;
    private Long bookingId; // hữu ích để operator bấm sang xem booking liên quan
}