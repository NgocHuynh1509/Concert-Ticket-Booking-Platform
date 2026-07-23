package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.TicketHold;
import com.example.concert_ticket_booking_platform.Entity.enums.HoldStatus;
import com.example.concert_ticket_booking_platform.Repository.TicketHoldRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
public class HoldExpiryScheduler {

    private final TicketHoldRepo ticketHoldRepository;
    private final TicketStockService ticketStockService;


    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireOverdueHolds() {
        List<TicketHold> overdue = ticketHoldRepository.findByStatusAndExpiresAtBefore(
                HoldStatus.HELD, LocalDateTime.now());

        for (TicketHold hold : overdue) {
            ticketStockService.releaseStock(hold.getTicketCategory().getId(), hold.getQuantity());
            hold.setStatus(HoldStatus.EXPIRED);
            ticketHoldRepository.save(hold);
        }
    }
}
