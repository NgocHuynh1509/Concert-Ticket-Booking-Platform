package com.example.concert_ticket_booking_platform.dto.concert;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Dung cho endpoint POST /api/concerts (chi OPERATOR/ADMIN duoc goi - xem SecurityConfig).
 * Khong nam trong spec /concerts GET duoc giao, nhung can de demo phan quyen dashboard
 * dung nhu comment trong Entity User/Concert.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateConcertRequest {

    @NotBlank(message = "name khong duoc de trong")
    private String name;

    private String description;

    @NotBlank(message = "venue khong duoc de trong")
    private String venue;

    @NotNull(message = "eventDate khong duoc de trong")
    @Future(message = "eventDate phai la thoi diem trong tuong lai")
    private LocalDateTime eventDate;

    private String concertMapUrl;

    private String posterUrl;
}