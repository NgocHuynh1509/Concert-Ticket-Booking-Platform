package com.example.concert_ticket_booking_platform.exception.payment;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
