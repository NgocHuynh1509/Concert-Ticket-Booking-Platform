package com.example.concert_ticket_booking_platform.Entity;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;


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