package com.example.concert_ticket_booking_platform.dto.concert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcertListResponse {
    private List<ConcertSummaryResponse> concerts;
}