package com.example.concert_ticket_booking_platform.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bảng ghi vết mỗi lần 1 voucher được áp dụng thành công vào 1 booking.
 * Lý do tách bảng riêng thay vì chỉ dùng Voucher.usedCount:
 *   - usedCount ở Voucher chỉ trả lời "còn lượt tổng hay không" (nhanh, 1 cột).
 *   - VoucherUsage trả lời "user X đã dùng voucher này bao nhiêu lần" (cho perUserLimit)
 *     và là bằng chứng audit khi operator cần điều tra booking nghi vấn gian lận voucher.
 *
 * Ràng buộc unique (voucher_id, booking_id): 1 booking chỉ áp dụng 1 voucher 1 lần,
 * tránh việc retry request tạo ra nhiều bản ghi usage cho cùng 1 booking.
 */
@Getter
@Setter
@Entity
@Table(name = "voucher_usages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_voucher_usage_booking", columnNames = {"voucher_id", "booking_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
}

