package com.example.concert_ticket_booking_platform.Entity;

import com.example.concert_ticket_booking_platform.Entity.enums.PaymentMethod;
import com.example.concert_ticket_booking_platform.Entity.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment tách khỏi Booking (Many-to-One thay vì 1-1 cứng) vì lý do rất thực tế:
 * user thanh toán thất bại lần 1 (thẻ bị từ chối) thì vẫn được thử lại lần 2 mà KHÔNG cần
 * tạo booking mới (booking vẫn giữ chỗ nhờ expiresAt còn hiệu lực) -> mỗi lần thử là 1 dòng
 * Payment riêng, dễ audit toàn bộ lịch sử giao dịch của 1 booking.
 *
 * ASSUMPTION: không tích hợp cổng thanh toán thật. API "confirm payment" trong scope test
 * sẽ nhận kết quả giả lập (mock gateway callback) để chuyển Payment.status và kéo theo
 * Booking.status tương ứng (SUCCESS -> Booking.PAID, FAILED -> Booking vẫn PENDING_PAYMENT
 * cho tới khi hết hạn hoặc thử lại thành công).
 *
 * transactionRef: mã tham chiếu phía cổng thanh toán (mock), dùng để đối soát & chống xử lý
 * callback trùng lặp (idempotent webhook) — cùng nguyên tắc idempotency key như ở Booking.
 */
@Getter
@Setter
@Entity
@Table(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_ref", unique = true, length = 100)
    private String transactionRef;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}

