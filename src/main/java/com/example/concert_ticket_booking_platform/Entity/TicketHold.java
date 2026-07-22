package com.example.concert_ticket_booking_platform.Entity;

import com.example.concert_ticket_booking_platform.Entity.enums.HoldStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Đại diện cho 1 lượt "giữ chỗ" tạm — trừ kho vé ngay lập tức nhưng CHƯA tạo Booking/Payment.
 * User có `expiresAt` (thường ngắn hơn nhiều so với thời gian giữ chỗ của Booking, vì đây chỉ
 * là bước "khoá vé trong lúc user điền thông tin/chọn voucher/thanh toán") để confirm; nếu
 * không confirm kịp, HoldExpiryScheduler sẽ hoàn lại kho.
 *
 * NOTE (giả định BaseEntity đã có sẵn trong project của bạn, cung cấp id/createdAt/updatedAt).
 * Nếu bạn chưa có class này, chỉ cần thêm 3 field đó trực tiếp vào đây.
 */
@Getter
@Setter
@Entity
@Table(name = "ticket_holds", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ticket_holds_idempotency_key", columnNames = "idempotency_key")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHold extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_category_id", nullable = false)
    private TicketCategory ticketCategory;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Gán sau khi confirm thành công — cho phép confirm lại (retry) trả về đúng booking cũ
    // thay vì tạo booking mới, mà không cần thêm idempotency key riêng cho bước confirm.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
