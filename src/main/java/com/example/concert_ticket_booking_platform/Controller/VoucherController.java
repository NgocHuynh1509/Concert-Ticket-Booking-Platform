package com.example.concert_ticket_booking_platform.Controller;

import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Entity.Voucher;
import com.example.concert_ticket_booking_platform.Repository.VoucherRepo;
import com.example.concert_ticket_booking_platform.Repository.VoucherUsageRepo;
import com.example.concert_ticket_booking_platform.dto.booking.VoucherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherRepo voucherRepo;
    private final VoucherUsageRepo voucherUsageRepo;

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableVouchers(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        LocalDateTime now = LocalDateTime.now();
        List<Voucher> activeVouchers = voucherRepo.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                .filter(v -> !now.isBefore(v.getValidFrom()) && !now.isAfter(v.getValidTo()))
                .toList();

        List<VoucherResponse> response = activeVouchers.stream()
                .map(v -> {
                    boolean isUsable = true;
                    String reason = null;

                    if (v.getUsedCount() >= v.getMaxUsage()) {
                        isUsable = false;
                        reason = "Đã hết lượt sử dụng";
                    } else {
                        long usedByThisUser = voucherUsageRepo.countByVoucherAndUser(v, currentUser);
                        if (usedByThisUser >= v.getPerUserLimit()) {
                            isUsable = false;
                            reason = "Bạn đã dùng tối đa số lượt";
                        }
                    }

                    return new VoucherResponse(v, isUsable, reason);
                })
                .toList();

        return ResponseEntity.ok(response);
    }
}