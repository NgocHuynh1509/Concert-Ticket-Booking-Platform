package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.Booking;

public record BookingResult(Booking booking, boolean alreadyExists) {

    // Thêm method này để dùng result.isReplay() ở Controller
    public boolean isReplay() {
        return alreadyExists;
    }
}