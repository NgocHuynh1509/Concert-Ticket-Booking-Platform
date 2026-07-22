package com.example.concert_ticket_booking_platform.dto.hold;

import com.example.concert_ticket_booking_platform.Entity.enums.HoldStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldResponse {

    private Long id;
    private Long ticketCategoryId;
    private String ticketCategoryName;
    private Integer quantity;
    private HoldStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Long bookingId; // null cho tới khi confirm

    // true nếu đây là kết quả trả về do trùng Idempotency-Key (không tạo hold mới)
    private boolean isReplay;
}
