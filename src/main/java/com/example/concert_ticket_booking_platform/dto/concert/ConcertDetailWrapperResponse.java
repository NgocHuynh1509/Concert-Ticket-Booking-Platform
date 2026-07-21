package com.example.concert_ticket_booking_platform.dto.concert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dung dung shape ma spec API yeu cau: { "concert": {..., ticketCategories: [...]} }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcertDetailWrapperResponse {
    private ConcertDetailResponse concert;
}