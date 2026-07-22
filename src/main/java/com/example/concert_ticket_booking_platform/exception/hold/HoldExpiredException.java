package com.example.concert_ticket_booking_platform.exception.hold;

// Map sang HTTP 410 (Gone) trong GlobalExceptionHandler
public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(String message) {
        super(message);
    }
}
