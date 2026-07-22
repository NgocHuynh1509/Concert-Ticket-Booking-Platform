package com.example.concert_ticket_booking_platform.dto.hold;

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
public class ConfirmHoldRequest {

    // Optional — user có thể áp voucher ngay lúc confirm (không bắt buộc lúc tạo hold).
    private String voucherCode;
}
