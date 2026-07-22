package com.example.concert_ticket_booking_platform.dto.voucher;



import com.example.concert_ticket_booking_platform.Entity.enums.DiscountType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer maxUsage;
    private Integer usedCount;
    private Integer perUserLimit;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean active;
}