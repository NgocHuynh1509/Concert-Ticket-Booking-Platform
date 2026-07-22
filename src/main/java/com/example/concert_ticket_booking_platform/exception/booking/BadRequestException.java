package com.example.concert_ticket_booking_platform.exception.booking;

/** 400 — lỗi input tổng quát (không thuộc các case cụ thể khác như validation @Valid). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
