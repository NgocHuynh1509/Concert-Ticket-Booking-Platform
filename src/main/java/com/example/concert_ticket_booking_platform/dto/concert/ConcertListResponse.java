package com.example.concert_ticket_booking_platform.dto.concert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Dung dung shape ma spec API yeu cau: { "concerts": Concert[] }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcertListResponse {
    private List<ConcertSummaryResponse> concerts;
}