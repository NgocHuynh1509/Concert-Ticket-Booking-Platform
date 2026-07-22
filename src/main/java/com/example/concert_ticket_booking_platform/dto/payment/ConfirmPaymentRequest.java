package com.example.concert_ticket_booking_platform.dto.payment;

import com.example.concert_ticket_booking_platform.Entity.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
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
public class ConfirmPaymentRequest {

    // Chỉ chấp nhận SUCCESS hoặc FAILED — service sẽ validate thêm (PENDING/REFUNDED không hợp lệ ở đây).
    // ASSUMPTION: không tích hợp cổng thật, đây là API mock để giả lập kết quả gateway trả về.
    @NotNull(message = "result không được để trống")
    private PaymentStatus result;
}
