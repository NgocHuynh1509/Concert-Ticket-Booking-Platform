package com.example.concert_ticket_booking_platform.Entity;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root của luồng đặt vé.
 *
 * - idempotencyKey (unique): client (web/app) tự sinh 1 key duy nhất cho mỗi lần bấm "Đặt vé"
 *   (ví dụ UUID lưu ở client trước khi gọi API) và gửi kèm request. Nếu request bị timeout và
 *   client tự động retry với CÙNG key, DB sẽ chặn insert trùng nhờ unique constraint, tầng
 *   service bắt lỗi này và trả về booking đã tạo trước đó thay vì tạo booking mới -> chống
 *   duplicate booking do retry, đúng vấn đề đề bài nêu.
 *
 * - expiresAt: thời điểm hết hạn giữ chỗ khi status = PENDING_PAYMENT. Một scheduled job
 *   (ngoài scope code chi tiết của test, chỉ cần nêu trong doc) sẽ quét các booking quá hạn,
 *   chuyển sang EXPIRED và hoàn lại availableQuantity cho TicketCategory.
 *
 * - totalAmount / discountAmount / finalAmount: lưu lại tại thời điểm đặt vé (snapshot),
 *   không tính lại từ giá hiện tại của TicketCategory/Voucher — giá vé hay % giảm giá có thể
 *   đổi sau này nhưng không được ảnh hưởng ngược tới booking đã tạo.
 */
@Getter
@Setter
@Entity
@Table(name = "bookings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bookings_idempotency_key", columnNames = "idempotency_key")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher; // nullable — không phải booking nào cũng dùng voucher

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingItem> items = new ArrayList<>();

    // 1 booking có thể có NHIỀU lần thử thanh toán (payment FAILED rồi thử lại),
    // nên đây không phải OneToOne cứng — xem giải thích ở Payment.java.
    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();
}
