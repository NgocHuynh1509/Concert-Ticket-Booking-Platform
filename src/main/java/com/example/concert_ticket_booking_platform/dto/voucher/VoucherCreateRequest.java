package com.example.concert_ticket_booking_platform.dto.voucher;


import com.example.concert_ticket_booking_platform.Entity.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VoucherCreateRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50, message = "Mã voucher tối đa 50 ký tự")
    private String code;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    @NotNull(message = "Số lượt sử dụng tối đa không được để trống")
    @Min(value = 1, message = "Số lượt sử dụng tối đa phải >= 1")
    private Integer maxUsage;

    @NotNull(message = "Giới hạn số lượt/user không được để trống")
    @Min(value = 1, message = "Giới hạn số lượt/user phải >= 1")
    private Integer perUserLimit;

    @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
    private LocalDateTime validFrom;

    @NotNull(message = "Ngày kết thúc hiệu lực không được để trống")
    private LocalDateTime validTo;
}