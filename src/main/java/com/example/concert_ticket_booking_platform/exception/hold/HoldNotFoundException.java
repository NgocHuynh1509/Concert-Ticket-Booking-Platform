package com.example.concert_ticket_booking_platform.exception.hold;

// Map sang HTTP 404 trong GlobalExceptionHandler
public class HoldNotFoundException extends RuntimeException {
    public HoldNotFoundException(String message) {
        super(message);
    }
}
