package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.Booking;
import com.example.concert_ticket_booking_platform.Entity.Payment;
import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentStatus;
import com.example.concert_ticket_booking_platform.Repository.BookingRepo;
import com.example.concert_ticket_booking_platform.Repository.PaymentRepo;
import com.example.concert_ticket_booking_platform.dto.booking.BookingResponse;
import com.example.concert_ticket_booking_platform.dto.payment.ConfirmPaymentRequest;
import com.example.concert_ticket_booking_platform.exception.payment.InvalidPaymentResultException;
import com.example.concert_ticket_booking_platform.exception.payment.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepo paymentRepository;
    private final BookingRepo bookingRepository;
    private final BookingService bookingService;

    @Transactional
    public BookingResponse confirmPayment(Long paymentId, ConfirmPaymentRequest request, User currentUser) {

        if (request.getResult() != PaymentStatus.SUCCESS && request.getResult() != PaymentStatus.FAILED) {
            throw new InvalidPaymentResultException("result chỉ được là SUCCESS hoặc FAILED");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Không tìm thấy payment"));

        Booking booking = payment.getBooking();

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new InvalidPaymentResultException("Bạn không có quyền xác nhận thanh toán này");
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new InvalidPaymentResultException(
                    "Booking không còn chờ thanh toán (trạng thái hiện tại: " + booking.getStatus() + ")");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return bookingService.getBookingResponse(booking.getId(), currentUser);
        }


        if (request.getResult() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            booking.setStatus(BookingStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);
        bookingRepository.save(booking);

        return bookingService.getBookingResponse(booking.getId(), currentUser);
    }
}
