package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.dto.voucher.VoucherCreateRequest;
import com.example.concert_ticket_booking_platform.dto.voucher.VoucherResponse;
import com.example.concert_ticket_booking_platform.dto.voucher.VoucherUsageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IVoucherService {

    Page<VoucherResponse> getVouchers(Pageable pageable);
    VoucherResponse createVoucher(VoucherCreateRequest request);
    VoucherResponse deactivateVoucher(Long id);
    List<VoucherUsageResponse> getVoucherUsages(Long voucherId);
}
