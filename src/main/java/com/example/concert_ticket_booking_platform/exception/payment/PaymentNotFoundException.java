package com.example.concert_ticket_booking_platform.exception.payment;

// Map sang HTTP 404 trong GlobalExceptionHandler
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
