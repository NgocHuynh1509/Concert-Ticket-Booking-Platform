package com.example.concert_ticket_booking_platform.dto.booking;

import com.example.concert_ticket_booking_platform.Entity.Voucher;
import com.example.concert_ticket_booking_platform.Entity.enums.DiscountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VoucherResponse {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime validTo;
    private boolean isUsable;
    private String reason;

    public VoucherResponse(Voucher voucher, boolean isUsable, String reason) {
        this.id = voucher.getId();
        this.code = voucher.getCode();
        this.discountType = voucher.getDiscountType();
        this.discountValue = voucher.getDiscountValue();
        this.validTo = voucher.getValidTo();
        this.isUsable = isUsable;
        this.reason = reason;
    }
}