package com.example.concert_ticket_booking_platform.exception.booking;

public class VoucherInvalidException extends RuntimeException {
    public VoucherInvalidException(String message) {
        super(message);
    }
}