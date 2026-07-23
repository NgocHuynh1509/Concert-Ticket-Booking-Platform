package com.example.concert_ticket_booking_platform.Entity;

import com.example.concert_ticket_booking_platform.Entity.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "vouchers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vouchers_code", columnNames = "code")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_usage", nullable = false)
    private Integer maxUsage;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "per_user_limit", nullable = false)
    @Builder.Default
    private Integer perUserLimit = 1;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version;
}

