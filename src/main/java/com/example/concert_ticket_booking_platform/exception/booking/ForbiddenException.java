package com.example.concert_ticket_booking_platform.exception.booking;

/** 403 — user đang đăng nhập không phải chủ sở hữu tài nguyên đang truy cập. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
