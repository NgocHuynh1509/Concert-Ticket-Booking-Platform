package com.example.concert_ticket_booking_platform.dto.voucher;

import com.example.concert_ticket_booking_platform.Entity.Voucher;
import com.example.concert_ticket_booking_platform.Entity.enums.DiscountType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class VoucherResponse {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime validTo;

    public VoucherResponse(Voucher voucher) {
        this.id = voucher.getId();
        this.code = voucher.getCode();
        this.discountType = voucher.getDiscountType();
        this.discountValue = voucher.getDiscountValue();
        this.validTo = voucher.getValidTo();
    }
}