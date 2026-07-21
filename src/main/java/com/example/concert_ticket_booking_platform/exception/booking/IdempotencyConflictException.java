package com.example.concert_ticket_booking_platform.exception.booking;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}