package com.example.concert_ticket_booking_platform.exception.booking;


public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}