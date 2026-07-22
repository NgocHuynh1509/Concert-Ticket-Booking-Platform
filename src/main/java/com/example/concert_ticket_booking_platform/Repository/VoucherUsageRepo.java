package com.example.concert_ticket_booking_platform.Repository;



import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Entity.Voucher;
import com.example.concert_ticket_booking_platform.Entity.VoucherUsage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherUsageRepo extends JpaRepository<VoucherUsage, Long> {
    long countByVoucherAndUser(Voucher voucher, User user);
    // EntityGraph để fetch sẵn User trong 1 query, tránh N+1 khi map sang DTO
    @EntityGraph(attributePaths = {"user"})
    List<VoucherUsage> findByVoucherIdOrderByCreatedAtDesc(Long voucherId);

}