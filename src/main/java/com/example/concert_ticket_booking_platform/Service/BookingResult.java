package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.Booking;

/**
 * alreadyExists = true khi request là "replay" của một Idempotency-Key đã xử lý trước đó
 * (payload giống hệt) -> Controller trả 200 kèm booking cũ thay vì 201 tạo mới.
 */
public record BookingResult(Booking booking, boolean alreadyExists) {

    // Thêm method này để dùng result.isReplay() ở Controller
    public boolean isReplay() {
        return alreadyExists;
    }
}