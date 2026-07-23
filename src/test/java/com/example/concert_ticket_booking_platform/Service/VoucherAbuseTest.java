package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.Booking;
import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Entity.Voucher;
import com.example.concert_ticket_booking_platform.Entity.enums.BookingStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.DiscountType;
import com.example.concert_ticket_booking_platform.Entity.enums.UserRole;
import com.example.concert_ticket_booking_platform.Repository.BookingRepo;
import com.example.concert_ticket_booking_platform.Repository.UserRepo;
import com.example.concert_ticket_booking_platform.Repository.VoucherRepo;
import com.example.concert_ticket_booking_platform.Repository.VoucherUsageRepo;
import com.example.concert_ticket_booking_platform.exception.booking.VoucherInvalidException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class VoucherAbuseTest {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepo voucherRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private VoucherUsageRepo voucherUsageRepo;

    @Autowired
    private BookingRepo bookingRepo;

    private User user;

    @BeforeEach
    void setUp() {
        voucherUsageRepo.deleteAll();
        bookingRepo.deleteAll();
        voucherRepo.deleteAll();
        userRepo.deleteAll();

        user = User.builder()
                .username("test_voucher")
                .email("test_voucher@gmail.com")
                .passwordHash("hash")
                .role(UserRole.CUSTOMER)
                .build();
        user = userRepo.save(user);
    }

    @AfterEach
    void tearDown() {
        voucherUsageRepo.deleteAll();
        bookingRepo.deleteAll();
        voucherRepo.deleteAll();
        userRepo.deleteAll();
    }


    @Test
    void case3b_voucherExpired_shouldThrowException() {
        System.out.println("\n=== [Voucher Test] Starting case3b: voucher expired ===");
        Voucher voucher = createVoucher("PAST", LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1), 10, 1, true);
        assertThrows(VoucherInvalidException.class, () -> voucherService.validateAndLoadVoucher(voucher.getCode(), user));
    }


    @Test
    void case3f_concurrentVoucherUsage_maxUsage2_5Users_shouldOnlyAllow2() throws InterruptedException {
        System.out.println("\n=== [Voucher Test] Starting case3f: concurrent usages (5 users, max 2) ===");
        Voucher voucher = createVoucher("RACE", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), 2, 1, true); // max 2
        int threads = 5;

        // Create 5 different users
        User[] users = new User[threads];
        for (int i = 0; i < threads; i++) {
            User u = User.builder()
                    .username("u" + i)
                    .email("u" + i + "@gmail.com")
                    .passwordHash("h")
                    .role(UserRole.CUSTOMER)
                    .build();
            users[i] = userRepo.save(u);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final User u = users[i];
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    // First validate
                    Voucher v = voucherService.validateAndLoadVoucher(voucher.getCode(), u);
                    // Then increment
                    voucherService.incrementVoucherUsage(v);
                    successCount.incrementAndGet();
                } catch (VoucherInvalidException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        
        System.out.println("[case3f] Finished. Success: " + successCount.get() + ", Fail: " + failCount.get());

        assertEquals(2, successCount.get(), "Exactly 2 usages should succeed");
        assertEquals(3, failCount.get(), "Exactly 3 usages should fail");

        Voucher updatedVoucher = voucherRepo.findById(voucher.getId()).orElseThrow();
        assertEquals(2, updatedVoucher.getUsedCount(), "Used count should be 2");
    }

    private Voucher createVoucher(String code, LocalDateTime from, LocalDateTime to, int maxUsage, int perUserLimit, boolean active) {
        Voucher voucher = Voucher.builder()
                .code(code)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .maxUsage(maxUsage)
                .usedCount(0)
                .perUserLimit(perUserLimit)
                .validFrom(from)
                .validTo(to)
                .active(active)
                .version(0L)
                .build();
        return voucherRepo.save(voucher);
    }
}
