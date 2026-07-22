package com.example.concert_ticket_booking_platform.Repository;

import com.example.concert_ticket_booking_platform.Entity.TicketHold;
import com.example.concert_ticket_booking_platform.Entity.enums.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketHoldRepo extends JpaRepository<TicketHold, Long> {

    Optional<TicketHold> findByIdempotencyKey(String idempotencyKey);

    // Dùng bởi HoldExpiryScheduler để quét các hold quá hạn.
    List<TicketHold> findByStatusAndExpiresAtBefore(HoldStatus status, LocalDateTime time);
}
