package com.example.concert_ticket_booking_platform.Entity;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Audit trail cho mỗi lần OPERATOR/ADMIN đổi trạng thái booking thủ công.
 * KHÔNG dùng cho các chuyển trạng thái tự động của hệ thống (vd: PENDING_PAYMENT -> PAID
 * do payment callback, hay -> EXPIRED do scheduled job) — chỉ ghi nhận hành vi có người can thiệp,
 * nên "reason" là bắt buộc để phục vụ đối soát / khiếu nại sau này.
 */
@Getter
@Setter
@Entity
@Table(name = "booking_status_histories")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private BookingStatus toStatus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;
}