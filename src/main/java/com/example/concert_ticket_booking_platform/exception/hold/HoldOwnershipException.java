package com.example.concert_ticket_booking_platform.exception.hold;

// Map sang HTTP 403 trong GlobalExceptionHandler
public class HoldOwnershipException extends RuntimeException {
    public HoldOwnershipException(String message) {
        super(message);
    }
}
