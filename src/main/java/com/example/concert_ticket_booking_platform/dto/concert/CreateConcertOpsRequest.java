package com.example.concert_ticket_booking_platform.dto.concert;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateConcertOpsRequest {

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

    @NotEmpty(message = "ticketCategories phai co it nhat 1 hang muc ve")
    @Valid
    private List<TicketCategoryCreateRequest> ticketCategories;
}
