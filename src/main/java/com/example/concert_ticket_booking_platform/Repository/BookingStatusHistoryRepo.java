package com.example.concert_ticket_booking_platform.Repository;

import com.example.concert_ticket_booking_platform.Entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStatusHistoryRepo extends JpaRepository<BookingStatusHistory, Long> {
    List<BookingStatusHistory> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}