package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OperatorBookingDetailResponse {
    private Long userId;
    private String userEmail;
    private String userFullName;

    private Long bookingId;
    private BookingStatus status;
    private String idempotencyKey;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String voucherCode;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private List<OperatorBookingItemDetail> items;

    private List<PaymentResponse> payments;
    private List<BookingStatusHistoryResponse> statusHistory;
}