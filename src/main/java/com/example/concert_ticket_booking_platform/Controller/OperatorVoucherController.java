package com.example.concert_ticket_booking_platform.Controller;

import com.example.concert_ticket_booking_platform.dto.voucher.VoucherCreateRequest;
import com.example.concert_ticket_booking_platform.dto.voucher.VoucherResponse;
import com.example.concert_ticket_booking_platform.dto.voucher.VoucherUsageResponse;
import com.example.concert_ticket_booking_platform.exception.VoucherException;
import com.example.concert_ticket_booking_platform.Service.IVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/ops/vouchers")
@RequiredArgsConstructor
public class OperatorVoucherController {

    private final IVoucherService voucherService;

    @GetMapping
    public ResponseEntity<Page<VoucherResponse>> getVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(voucherService.getVouchers(pageable));
    }

    @PostMapping
    public ResponseEntity<VoucherResponse> createVoucher(
            @Valid @RequestBody VoucherCreateRequest request
    ) {
        VoucherResponse response = voucherService.createVoucher(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<VoucherResponse> deactivateVoucher(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.deactivateVoucher(id));
    }

    @ExceptionHandler(VoucherException.class)
    public ResponseEntity<String> handleVoucherException(VoucherException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @GetMapping("/{id}/usages")
    public ResponseEntity<List<VoucherUsageResponse>> getUsages(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.getVoucherUsages(id));
    }
}