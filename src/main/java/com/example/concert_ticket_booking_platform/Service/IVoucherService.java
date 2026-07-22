package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.dto.voucher.VoucherCreateRequest;
import com.example.concert_ticket_booking_platform.dto.voucher.VoucherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IVoucherService {

    Page<VoucherResponse> getVouchers(Pageable pageable);

    VoucherResponse createVoucher(VoucherCreateRequest request);

    VoucherResponse deactivateVoucher(Long id);
}
