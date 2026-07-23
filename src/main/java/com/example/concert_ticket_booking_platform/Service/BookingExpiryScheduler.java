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


@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingRepo bookingRepository;
    private final BookingItemRepo bookingItemRepository;
    private final PaymentRepo paymentRepository;
    private final TicketStockService ticketStockService;

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