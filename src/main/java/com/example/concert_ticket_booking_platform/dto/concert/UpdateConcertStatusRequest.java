package com.example.concert_ticket_booking_platform.dto.concert;

import com.example.concert_ticket_booking_platform.Entity.enums.ConcertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConcertStatusRequest {

    @NotNull(message = "status khong duoc de trong")
    private ConcertStatus status;
}
