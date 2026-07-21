package com.example.concert_ticket_booking_platform.Repository;



import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Entity.Voucher;
import com.example.concert_ticket_booking_platform.Entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherUsageRepo extends JpaRepository<VoucherUsage, Long> {
    long countByVoucherAndUser(Voucher voucher, User user);

}