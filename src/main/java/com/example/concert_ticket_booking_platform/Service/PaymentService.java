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

/**
 * ASSUMPTION: không tích hợp cổng thanh toán thật (VNPay/Momo/Stripe) trong bản này.
 * confirmPayment() đóng vai trò "webhook giả lập" — thay vì chờ callback từ gateway thật,
 * frontend (trang mock thanh toán) gọi trực tiếp endpoint này với kết quả SUCCESS/FAILED.
 * Khi tích hợp VNPay thật sau này, chỉ cần thay nguồn gọi (từ nút bấm -> callback VNPay ký số),
 * logic xử lý trạng thái bên dưới giữ nguyên.
 */
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

        // SUCCESS là trạng thái cuối — không cho lật ngược lại FAILED sau khi đã thanh toán xong.
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return bookingService.getBookingResponse(booking.getId(), currentUser);
        }

        // Cho phép FAILED -> SUCCESS (mô phỏng user bấm "thất bại" rồi thử lại và "thành công"),
        // vì bản này dùng chung 1 payment cho mỗi booking thay vì tạo payment mới mỗi lần retry.
        if (request.getResult() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            booking.setStatus(BookingStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            // Booking giữ nguyên PENDING_PAYMENT — user còn có thể thử lại trong lúc chưa hết hạn giữ chỗ.
        }

        paymentRepository.save(payment);
        bookingRepository.save(booking);

        return bookingService.getBookingResponse(booking.getId(), currentUser);
    }
}
