package com.example.concert_ticket_booking_platform.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "ticket_categories")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCategory extends BaseEntity {
    @ToString.Exclude
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


