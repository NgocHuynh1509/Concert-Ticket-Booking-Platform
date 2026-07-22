package com.example.concert_ticket_booking_platform.exception.booking;

/**
 * 404 — dùng khi ticketCategoryId/concertId/voucherCode/bookingId trong request
 * không tồn tại trong DB. Tách riêng khỏi VoucherInvalidException vì đây là lỗi
 * "không tìm thấy resource" (404), còn VoucherInvalidException là lỗi nghiệp vụ
 * "resource tồn tại nhưng không dùng được" (400).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}