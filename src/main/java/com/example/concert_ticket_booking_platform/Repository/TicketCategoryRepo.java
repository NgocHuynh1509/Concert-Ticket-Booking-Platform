package com.example.concert_ticket_booking_platform.Repository;

import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketCategoryRepo extends JpaRepository<TicketCategory, Long> {

    List<TicketCategory> findByConcertId(Long concertId);
    List<TicketCategory> findAllByConcertId(Long concertId);
    @Query("""
        SELECT tc FROM TicketCategory tc
        WHERE (:concertId IS NULL OR tc.concert.id = :concertId)
          AND (:name IS NULL OR LOWER(tc.name) LIKE LOWER(CONCAT('%', :name, '%')))
        ORDER BY tc.concert.id, tc.name
    """)
    List<TicketCategory> findByFilters(
            @Param("concertId") Long concertId,
            @Param("name") String name
    );
}