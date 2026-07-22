package com.example.concert_ticket_booking_platform.exception;


import org.springframework.http.HttpStatus;

public class VoucherException extends RuntimeException {

    private final HttpStatus status;

    public VoucherException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}