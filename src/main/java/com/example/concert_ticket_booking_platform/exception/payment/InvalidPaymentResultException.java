package com.example.concert_ticket_booking_platform.exception.payment;


public class InvalidPaymentResultException extends RuntimeException {
    public InvalidPaymentResultException(String message) {
        super(message);
    }
}
