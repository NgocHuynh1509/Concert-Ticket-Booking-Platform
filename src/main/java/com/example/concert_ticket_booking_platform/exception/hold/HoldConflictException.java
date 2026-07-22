package com.example.concert_ticket_booking_platform.exception.hold;

// Map sang HTTP 409 trong GlobalExceptionHandler — cùng Idempotency-Key nhưng khác payload
public class HoldConflictException extends RuntimeException {
    public HoldConflictException(String message) {
        super(message);
    }
}
