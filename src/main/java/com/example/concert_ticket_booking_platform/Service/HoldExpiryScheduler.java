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

/**
 * Tuyến phòng thủ thứ 2 cho hold hết hạn (tuyến thứ 1 là check expiresAt ngay trong
 * HoldService.confirmHold). Quét định kỳ các TicketHold còn HELD nhưng đã quá expiresAt,
 * hoàn lại kho vé và chuyển status -> EXPIRED.
 *
 * NOTE: cần bật @EnableScheduling ở class Application chính (@SpringBootApplication) nếu
 * project chưa có, nếu không @Scheduled sẽ không bao giờ chạy.
 */
@Component
@RequiredArgsConstructor
public class HoldExpiryScheduler {

    private final TicketHoldRepo ticketHoldRepository;
    private final TicketStockService ticketStockService;

    // Chạy mỗi 60s — đủ nhanh so với HOLD_MINUTES = 5 phút ở HoldService,
    // nghĩa là 1 hold hết hạn sẽ được nhả kho chậm nhất sau ~1 phút.
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
