package com.example.concert_ticket_booking_platform.Repository;

import com.example.concert_ticket_booking_platform.Entity.Booking;
import com.example.concert_ticket_booking_platform.Entity.BookingItem;
import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface BookingRepo extends JpaRepository<Booking, Long> {
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
    // 💡 Thêm dòng này vào:
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime time);
    // "Vé của tôi" — không filter status (tab "Tất cả")
    List<Booking> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // "Vé của tôi" — có filter status (tab PENDING_PAYMENT / PAID / EXPIRED / ...)
    List<Booking> findByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, BookingStatus status);


}