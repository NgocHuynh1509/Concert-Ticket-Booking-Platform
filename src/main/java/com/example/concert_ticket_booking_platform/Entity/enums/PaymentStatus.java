package com.example.concert_ticket_booking_platform.Entity.enums;

/**
 * Trạng thái của 1 lần thanh toán. Tách riêng khỏi BookingStatus vì:
 * - 1 booking có thể có nhiều lần thử thanh toán (retry sau khi FAILED)
 * - Payment là nơi lưu vết giao dịch thật (transactionRef) để đối soát, Booking chỉ phản ánh
 *   trạng thái nghiệp vụ tổng quát.
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
