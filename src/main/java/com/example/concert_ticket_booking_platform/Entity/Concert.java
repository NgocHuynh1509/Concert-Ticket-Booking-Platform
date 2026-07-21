package com.example.concert_ticket_booking_platform.Entity;

import com.example.concert_ticket_booking_platform.Entity.enums.ConcertStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "concerts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Concert extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConcertStatus status;

    // mappedBy vì TicketCategory là chủ sở hữu FK (concert_id nằm bên đó).
    // cascade PERSIST/MERGE để operator tạo concert kèm category trong 1 request tiện lợi hơn,
    // nhưng KHÔNG cascade REMOVE — xoá concert không nên tự động xoá category đang có booking.
    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "concert", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<TicketCategory> ticketCategories = new ArrayList<>();
}
