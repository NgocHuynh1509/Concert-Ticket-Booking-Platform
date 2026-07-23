package com.example.concert_ticket_booking_platform.exception.hold;

public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(String message) {
        super(message);
    }
}
