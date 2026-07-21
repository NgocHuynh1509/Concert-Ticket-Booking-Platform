package com.example.concert_ticket_booking_platform.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Format lỗi thống nhất cho toàn bộ API (400/409/...).
 * errors: dùng khi lỗi validation có nhiều field (@Valid), null nếu chỉ có 1 message chung.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String code;      // vd: OUT_OF_STOCK, VOUCHER_INVALID, IDEMPOTENCY_CONFLICT, VALIDATION_ERROR
    private String message;
    private List<String> errors;
}
