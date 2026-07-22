package com.example.concert_ticket_booking_platform.Repository;

import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCategoryRepo extends JpaRepository<TicketCategory, Long> {

    List<TicketCategory> findByConcertId(Long concertId);
    List<TicketCategory> findAllByConcertId(Long concertId);
}