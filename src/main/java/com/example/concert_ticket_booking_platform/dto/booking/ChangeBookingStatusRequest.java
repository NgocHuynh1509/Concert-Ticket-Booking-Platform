package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeBookingStatusRequest {
    @NotNull(message = "Trạng thái mới không được để trống")
    private BookingStatus status;

    @NotBlank(message = "Lý do đổi trạng thái không được để trống")
    @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
    private String reason;
}