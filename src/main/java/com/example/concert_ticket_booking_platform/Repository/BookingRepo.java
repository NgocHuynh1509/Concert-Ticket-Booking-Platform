package com.example.concert_ticket_booking_platform.Repository;

import com.example.concert_ticket_booking_platform.Entity.Booking;
import com.example.concert_ticket_booking_platform.Entity.BookingItem;
import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository

public interface BookingRepo extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime time);
    List<Booking> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, BookingStatus status);
}