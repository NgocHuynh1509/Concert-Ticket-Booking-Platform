package com.example.concert_ticket_booking_platform.exception.booking;


public class InsufficientTicketException extends RuntimeException {
    public InsufficientTicketException(String message) {
        super(message);
    }
}