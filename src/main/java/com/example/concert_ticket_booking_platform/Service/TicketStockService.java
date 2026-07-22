package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import com.example.concert_ticket_booking_platform.Repository.TicketCategoryRepo;
import com.example.concert_ticket_booking_platform.exception.booking.InsufficientTicketException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Tách riêng khỏi BookingService vì giờ có 2 nơi cần trừ kho (BookingService.createBooking
 * đặt trực tiếp, và HoldService.createHold giữ chỗ tạm) — và HoldService còn cần thêm thao tác
 * HOÀN kho (khi hold hết hạn/bị huỷ) mà trước đây BookingService chưa cần tới.
 * Giữ 1 nơi duy nhất quản lý optimistic-lock retry cho TicketCategory để tránh lệch logic.
 */
@Service
@RequiredArgsConstructor
public class TicketStockService {

    private static final int MAX_STOCK_RETRY = 2;

    private final TicketCategoryRepo ticketCategoryRepository;
    private final EntityManager entityManager;

    /**
     * Trừ kho vé — optimistic lock (@Version) + retry khi conflict.
     * Giữ nguyên logic gốc từ BookingService.reserveStock().
     */
    public TicketCategory reserveStock(Long ticketCategoryId, int quantity) {
        int attempt = 0;
        while (true) {
            attempt++;
            TicketCategory ticketCategory = ticketCategoryRepository.findById(ticketCategoryId)
                    .orElseThrow(() -> new InsufficientTicketException("Loại vé không tồn tại"));

            if (ticketCategory.getAvailableQuantity() < quantity) {
                throw new InsufficientTicketException("Vé đã hết hoặc không đủ số lượng yêu cầu");
            }
            ticketCategory.setAvailableQuantity(ticketCategory.getAvailableQuantity() - quantity);

            try {
                return ticketCategoryRepository.saveAndFlush(ticketCategory);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                entityManager.clear();
                if (attempt > MAX_STOCK_RETRY) {
                    throw new InsufficientTicketException(
                            "Vé vừa được người khác đặt, vui lòng thử lại");
                }
            }
        }
    }

    /**
     * Hoàn lại kho vé — dùng khi TicketHold hết hạn hoặc bị huỷ trước khi confirm thành Booking.
     * Chặn availableQuantity vượt quá totalQuantity để tránh lỗi cộng dồn sai (ví dụ hoàn 2 lần
     * do bug ở tầng gọi).
     */
    public void releaseStock(Long ticketCategoryId, int quantity) {
        int attempt = 0;
        while (true) {
            attempt++;
            TicketCategory ticketCategory = ticketCategoryRepository.findById(ticketCategoryId)
                    .orElseThrow(() -> new InsufficientTicketException("Loại vé không tồn tại"));

            int restored = Math.min(
                    ticketCategory.getTotalQuantity(),
                    ticketCategory.getAvailableQuantity() + quantity);
            ticketCategory.setAvailableQuantity(restored);

            try {
                ticketCategoryRepository.saveAndFlush(ticketCategory);
                return;
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                entityManager.clear();
                if (attempt > MAX_STOCK_RETRY) {
                    throw new InsufficientTicketException(
                            "Không thể hoàn lại kho vé lúc này, vui lòng thử lại");
                }
            }
        }
    }
}
