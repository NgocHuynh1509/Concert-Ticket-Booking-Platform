package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.Booking;
import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Entity.Voucher;
import com.example.concert_ticket_booking_platform.Entity.VoucherUsage;
import com.example.concert_ticket_booking_platform.Repository.VoucherRepo;
import com.example.concert_ticket_booking_platform.Repository.VoucherUsageRepo;
import com.example.concert_ticket_booking_platform.exception.booking.VoucherInvalidException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private static final int MAX_VOUCHER_RETRY = 2;

    private final VoucherRepo voucherRepository;
    private final VoucherUsageRepo voucherUsageRepository;

    public Voucher validateAndLoadVoucher(String code, User user) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new VoucherInvalidException("Voucher không tồn tại"));

        if (!Boolean.TRUE.equals(voucher.getActive())) {
            throw new VoucherInvalidException("Voucher hiện không khả dụng");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getValidFrom()) || now.isAfter(voucher.getValidTo())) {
            throw new VoucherInvalidException("Voucher đã hết hạn hoặc chưa tới thời gian áp dụng");
        }

        if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
            throw new VoucherInvalidException("Voucher đã hết lượt sử dụng");
        }

        long usedByThisUser = voucherUsageRepository.countByVoucherAndUser(voucher, user);
        if (usedByThisUser >= voucher.getPerUserLimit()) {
            throw new VoucherInvalidException("Bạn đã dùng hết số lần cho phép của voucher này");
        }

        return voucher;
    }

    public BigDecimal applyDiscount(Voucher voucher, BigDecimal totalAmount) {
        BigDecimal discount = switch (voucher.getDiscountType()) {
            case PERCENTAGE -> totalAmount
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> voucher.getDiscountValue();
        };
        return discount.min(totalAmount);
    }

    public Voucher incrementVoucherUsage(Voucher voucher) {
        int attempt = 0;
        while (true) {
            attempt++;
            if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
                throw new VoucherInvalidException("Voucher đã hết lượt sử dụng");
            }
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            try {
                voucher = voucherRepository.saveAndFlush(voucher);
                return voucher;
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                if (attempt > MAX_VOUCHER_RETRY) {
                    throw new VoucherInvalidException("Voucher vừa hết lượt, vui lòng thử lại");
                }
                voucher = voucherRepository.findById(voucher.getId())
                        .orElseThrow(() -> new VoucherInvalidException("Voucher không tồn tại"));
            }
        }
    }

    public void recordVoucherUsage(Voucher voucher, User currentUser, Booking savedBooking) {
        voucherUsageRepository.save(VoucherUsage.builder()
                .voucher(voucher)
                .user(currentUser)
                .booking(savedBooking)
                .build());
    }
}
