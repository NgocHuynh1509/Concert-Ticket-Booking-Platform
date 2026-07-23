package com.example.concert_ticket_booking_platform.exception.booking;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
