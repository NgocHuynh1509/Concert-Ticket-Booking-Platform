package com.example.concert_ticket_booking_platform.Entity.enums;

/**
 * State machine của 1 booking. Vì đề yêu cầu có API "update trạng thái booking thủ công",
 * transition hợp lệ nên được kiểm soát ở service layer (không cho nhảy trạng thái tuỳ ý), ví dụ:
 *
 *   PENDING_PAYMENT --(thanh toán thành công)--> PAID
 *   PENDING_PAYMENT --(hết hạn giữ chỗ / operator huỷ)--> EXPIRED / CANCELLED
 *   PENDING_PAYMENT --(thanh toán thất bại)--> FAILED
 *   PAID            --(operator huỷ thủ công, nghi vấn gian lận)--> CANCELLED
 *
 * PENDING_PAYMENT chính là lúc vé đã được "giữ chỗ" (trừ availableQuantity),
 * nên cần cơ chế hết hạn (expiresAt) để nhả vé lại nếu user không thanh toán -> tránh giữ chỗ ảo.
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    PAID,
    CANCELLED,
    FAILED,
    EXPIRED
}
