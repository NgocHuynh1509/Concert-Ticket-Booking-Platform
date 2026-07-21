package com.example.concert_ticket_booking_platform.Repository;

import com.example.concert_ticket_booking_platform.Entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepo extends JpaRepository<Voucher, Long> {

    // Tìm voucher theo Mã code (ví dụ: "SUMMER2026")
    Optional<Voucher> findByCode(String code);


}