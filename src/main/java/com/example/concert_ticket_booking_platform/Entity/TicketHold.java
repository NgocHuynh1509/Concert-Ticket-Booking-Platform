package com.example.concert_ticket_booking_platform.Entity;

import com.example.concert_ticket_booking_platform.Entity.enums.HoldStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
