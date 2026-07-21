package com.example.concert_ticket_booking_platform.dto.booking;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body của POST /api/bookings.
 * voucherCode là optional — không có @NotBlank, chỉ validate nếu người dùng có nhập
 * (xem BookingService: chuỗi rỗng/blank được coi như "không dùng voucher").
 *
 * Thiếu ticketCategoryId/quantity hoặc quantity <= 0 sẽ bị @Valid chặn ở Controller,
 * trả 400 tự động qua GlobalExceptionHandler (bind MethodArgumentNotValidException).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "ticketCategoryId là bắt buộc")
    private Long ticketCategoryId;

    @NotNull(message = "quantity là bắt buộc")
    @Min(value = 1, message = "quantity phải >= 1")
    private Integer quantity;

    private String voucherCode;
}