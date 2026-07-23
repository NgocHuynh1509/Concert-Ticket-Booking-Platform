package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.Concert;
import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import com.example.concert_ticket_booking_platform.Entity.enums.ConcertStatus;
import com.example.concert_ticket_booking_platform.Repository.ConcertRepo;
import com.example.concert_ticket_booking_platform.Repository.TicketCategoryRepo;
import com.example.concert_ticket_booking_platform.exception.booking.InsufficientTicketException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class TicketOversellTest {

    @Autowired
    private TicketStockService ticketStockService;

    @Autowired
    private TicketCategoryRepo ticketCategoryRepo;

    @Autowired
    private ConcertRepo concertRepo;

    private Long categoryAId;
    private Long categoryBId;
    private Concert concert;

    @BeforeEach
    void setUp() {
        ticketCategoryRepo.deleteAll();
        concertRepo.deleteAll();

        concert = Concert.builder()
                .name("Oversell Test Concert")
                .venue("Test Venue")
                .eventDate(LocalDateTime.now().plusDays(10))
                .status(ConcertStatus.PUBLISHED)
                .build();
        concertRepo.save(concert);

        TicketCategory categoryA = TicketCategory.builder()
                .concert(concert)
                .name("VIP")
                .price(BigDecimal.valueOf(1000))
                .totalQuantity(10)
                .availableQuantity(1) // Only 1 left!
                .version(0L)
                .build();
        categoryA = ticketCategoryRepo.save(categoryA);
        categoryAId = categoryA.getId();

        TicketCategory categoryB = TicketCategory.builder()
                .concert(concert)
                .name("Standard")
                .price(BigDecimal.valueOf(500))
                .totalQuantity(10)
                .availableQuantity(2) // 2 left
                .version(0L)
                .build();
        categoryB = ticketCategoryRepo.save(categoryB);
        categoryBId = categoryB.getId();
    }

    @AfterEach
    void tearDown() {
        ticketCategoryRepo.deleteAll();
        concertRepo.deleteAll();
    }

    @Test
    void case1a_twoThreads_oneTicketLeft_shouldOnlyAllowOne() throws InterruptedException {
        System.out.println("\n=== [Oversell Test] Starting case1a: 2 threads competing for 1 ticket ===");
        int threads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // wait until all threads are ready
                    ticketStockService.reserveStock(categoryAId, 1);
                    successCount.incrementAndGet();
                } catch (InsufficientTicketException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // start all threads at once
        doneLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();
        
        System.out.println("[case1a] Finished. Success count: " + successCount.get() + ", Fail count: " + failCount.get());

        assertTrue(successCount.get() <= 1, "Only up to 1 thread should succeed");
        assertTrue(failCount.get() >= 1, "At least 1 thread should fail");

        TicketCategory updatedCategory = ticketCategoryRepo.findById(categoryAId).orElseThrow();
        assertEquals(1 - updatedCategory.getAvailableQuantity(), successCount.get(), "Success count should exactly match deducted tickets");
    }

    @Test
    void case1b_fiveThreads_twoTicketsLeft_shouldOnlyAllowTwo() throws InterruptedException {
        System.out.println("\n=== [Oversell Test] Starting case1b: 5 threads competing for 2 tickets ===");
        int threads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    ticketStockService.reserveStock(categoryBId, 1);
                    successCount.incrementAndGet();
                } catch (InsufficientTicketException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();
        
        System.out.println("[case1b] Finished. Success count: " + successCount.get() + ", Fail count: " + failCount.get());

        assertTrue(successCount.get() <= 2, "Should never succeed more than 2 times");
        assertTrue(failCount.get() >= 3, "At least 3 threads must fail (either no stock or optimistic lock timeout)");

        TicketCategory updatedCategory = ticketCategoryRepo.findById(categoryBId).orElseThrow();
        assertEquals(2 - updatedCategory.getAvailableQuantity(), successCount.get(), "Success count should exactly match the deducted tickets");
    }

}
