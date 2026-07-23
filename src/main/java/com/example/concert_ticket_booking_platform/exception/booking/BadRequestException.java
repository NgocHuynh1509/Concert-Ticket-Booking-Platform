package com.example.concert_ticket_booking_platform.exception.booking;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
