package com.example.concert_ticket_booking_platform.Entity.enums;

/**
 * ASSUMPTION: không tích hợp cổng thanh toán thật (VNPay/Momo/Stripe) trong scope test này.
 * Payment sẽ được mock: 1 API "confirm payment" giả lập kết quả thành công/thất bại để
 * demo được toàn bộ luồng state transition. Enum này chỉ để lưu vết method người dùng "chọn".
 */
public enum PaymentMethod {
    CREDIT_CARD,
    MOMO,
    VNPAY,
    BANK_TRANSFER
}

