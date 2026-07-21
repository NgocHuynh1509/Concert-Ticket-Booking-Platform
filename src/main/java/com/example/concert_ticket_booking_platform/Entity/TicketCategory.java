package com.example.concert_ticket_booking_platform.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity quan trọng nhất cho bài toán chống oversell.
 *
 * - availableQuantity: số vé còn lại, bị trừ khi reserve.
 * - version: cột optimistic locking của JPA (@Version). Khi 2 request cùng UPDATE
 *   1 dòng TicketCategory, request thứ 2 sẽ nhận OptimisticLockException vì version
 *   đã bị request thứ 1 tăng lên trước đó -> tầng service bắt exception này và trả lỗi
 *   "hết vé / vui lòng thử lại" thay vì cho phép cả 2 request cùng trừ kho (race condition).
 *
 * TRADE-OFF đã cân nhắc: optimistic lock (chọn) vs pessimistic lock (SELECT ... FOR UPDATE)
 * vs Redis distributed lock:
 *   - Pessimistic lock giữ khoá DB lâu hơn -> dễ nghẽn khi 300-500 req/phút cùng lúc.
 *   - Optimistic lock phù hợp hơn vì conflict chỉ xảy ra khi 2 request đụng đúng 1 category
 *     cùng lúc, và ta có thể retry ở tầng service (retry-on-conflict) một cách rẻ.
 *   - Redis lock/queue là hướng mở rộng đúng cho traffic thật lớn hơn (xem doc Design),
 *     nhưng vượt scope "không cần production-ready" của bài test.
 */
@Getter
@Setter
@Entity
@Table(name = "ticket_categories")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false, length = 100)
    private String name; // VIP, Standard, Economy...

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Version
    @Column(nullable = false)
    private Long version;
}

