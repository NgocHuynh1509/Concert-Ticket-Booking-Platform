package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.Booking;
import com.example.concert_ticket_booking_platform.Entity.BookingItem;
import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentStatus;
import com.example.concert_ticket_booking_platform.Repository.BookingItemRepo;
import com.example.concert_ticket_booking_platform.Repository.BookingRepo;
import com.example.concert_ticket_booking_platform.Repository.PaymentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự động huỷ Booking còn PENDING_PAYMENT quá hạn giữ chỗ (Booking.expiresAt, mặc định 15 phút
 * kể từ lúc tạo — xem BOOKING_HOLD_MINUTES trong BookingService) mà vẫn chưa thanh toán xong.
 * Hoàn lại kho vé tương ứng, tránh giữ chỗ ảo vô thời hạn khi user rời trang /payment mà
 * không bấm nút nào (thành công lẫn thất bại).
 *
 * Đây là tuyến xử lý riêng cho Booking — khác với HoldExpiryScheduler (xử lý TicketHold, hạn 5
 * phút, giai đoạn TRƯỚC khi có Booking). Cả 2 cùng tồn tại vì ứng với 2 giai đoạn khác nhau
 * của luồng: Hold (đang chọn vé) -> Booking PENDING_PAYMENT (đang chờ thanh toán).
 *
 * ASSUMPTION: KHÔNG hoàn lại lượt dùng voucher (Voucher.usedCount / bản ghi VoucherUsage) khi
 * booking hết hạn — voucher được tính là "đã dùng" ngay lúc tạo booking, kể cả nếu booking sau
 * đó bị expired vì không thanh toán kịp. Nếu muốn hoàn lượt voucher trong trường hợp này (để
 * user/người khác dùng lại), cần bổ sung thêm bước decrement usedCount + xoá VoucherUsage
 * tương ứng ngay trong vòng lặp bên dưới.
 *
 * NOTE: cần @EnableScheduling ở class @SpringBootApplication (nếu chưa bật cho HoldExpiryScheduler).
 */
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingRepo bookingRepository;
    private final BookingItemRepo bookingItemRepository;
    private final PaymentRepo paymentRepository;
    private final TicketStockService ticketStockService;

    // Chạy mỗi 60s — đủ nhanh so với hạn giữ chỗ 15 phút của Booking.
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireOverdueBookings() {
        List<Booking> overdue = bookingRepository.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING_PAYMENT, LocalDateTime.now());

        for (Booking booking : overdue) {
            List<BookingItem> items = bookingItemRepository.findByBookingId(booking.getId());
            for (BookingItem item : items) {
                ticketStockService.releaseStock(item.getTicketCategory().getId(), item.getQuantity());
            }

            // Payment còn PENDING (chưa từng confirm SUCCESS/FAILED) -> đánh dấu FAILED để tránh
            // ops dashboard nhìn thấy payment "PENDING" treo lơ lửng ứng với 1 booking đã EXPIRED.
            paymentRepository.findByBookingId(booking.getId()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }
            });

            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
        }
    }
}