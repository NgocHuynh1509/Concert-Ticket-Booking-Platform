package com.example.concert_ticket_booking_platform.dto.concert;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketCategoryCreateRequest {

    @NotBlank(message = "ten ticket category khong duoc de trong")
    private String name;

    @NotNull(message = "price khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = true, message = "price khong duoc am")
    private BigDecimal price;

    @NotNull(message = "totalQuantity khong duoc de trong")
    @Min(value = 1, message = "totalQuantity phai lon hon 0")
    private Integer totalQuantity;
}
