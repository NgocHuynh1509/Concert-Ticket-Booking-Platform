package com.example.concert_ticket_booking_platform.exception.payment;

// Map sang HTTP 400 trong GlobalExceptionHandler (result không hợp lệ, booking không còn PENDING_PAYMENT,
// hoặc payment đã SUCCESS không cho đổi kết quả nữa)
public class InvalidPaymentResultException extends RuntimeException {
    public InvalidPaymentResultException(String message) {
        super(message);
    }
}
