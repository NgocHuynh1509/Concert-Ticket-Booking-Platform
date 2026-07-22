package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.dto.voucher.VoucherCreateRequest;
import com.example.concert_ticket_booking_platform.dto.voucher.VoucherResponse;
import com.example.concert_ticket_booking_platform.Entity.Voucher;
import com.example.concert_ticket_booking_platform.Entity.enums.DiscountType;
import com.example.concert_ticket_booking_platform.exception.VoucherException;
import com.example.concert_ticket_booking_platform.Repository.VoucherRepo;
import com.example.concert_ticket_booking_platform.Service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements IVoucherService {

    private final VoucherRepo voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getVouchers(Pageable pageable) {
        return voucherRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public VoucherResponse createVoucher(VoucherCreateRequest request) {
        validateCreateRequest(request);

        if (voucherRepository.existsByCode(request.getCode())) {
            throw new VoucherException(
                    "Mã voucher '" + request.getCode() + "' đã tồn tại",
                    HttpStatus.CONFLICT
            );
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode().trim().toUpperCase())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxUsage(request.getMaxUsage())
                .usedCount(0)
                .perUserLimit(request.getPerUserLimit())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .active(true)
                .build();

        Voucher saved = voucherRepository.save(voucher);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public VoucherResponse deactivateVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new VoucherException(
                        "Không tìm thấy voucher id=" + id, HttpStatus.NOT_FOUND));

        if (Boolean.FALSE.equals(voucher.getActive())) {
            // idempotent — deactivate lần 2 không lỗi, trả về state hiện tại
            return toResponse(voucher);
        }

        voucher.setActive(false);
        Voucher saved = voucherRepository.save(voucher);
        return toResponse(saved);
    }

    private void validateCreateRequest(VoucherCreateRequest request) {
        if (!request.getValidTo().isAfter(request.getValidFrom())) {
            throw new VoucherException(
                    "validTo phải sau validFrom", HttpStatus.BAD_REQUEST);
        }

        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new VoucherException(
                    "Giá trị giảm giá kiểu PERCENTAGE không được vượt quá 100",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (request.getPerUserLimit() > request.getMaxUsage()) {
            throw new VoucherException(
                    "perUserLimit không được lớn hơn maxUsage",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private VoucherResponse toResponse(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .maxUsage(v.getMaxUsage())
                .usedCount(v.getUsedCount())
                .perUserLimit(v.getPerUserLimit())
                .validFrom(v.getValidFrom())
                .validTo(v.getValidTo())
                .active(v.getActive())
                .build();
    }
}