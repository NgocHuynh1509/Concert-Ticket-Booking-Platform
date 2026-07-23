package com.example.concert_ticket_booking_platform.dto.concert;

import com.example.concert_ticket_booking_platform.Entity.enums.ConcertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcertOpsResponse {
    private Long concertId;
    private ConcertStatus status;
}
