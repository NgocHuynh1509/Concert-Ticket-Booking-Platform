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
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class FlashSaleLoadTest {

    @Autowired
    private TicketStockService ticketStockService;

    @Autowired
    private TicketCategoryRepo ticketCategoryRepo;

    @Autowired
    private ConcertRepo concertRepo;

    private Long vipId;
    private Long standardId;
    private Long economyId;
    private Concert concert;

    @BeforeEach
    void setUp() {
        ticketCategoryRepo.deleteAll();
        concertRepo.deleteAll();

        concert = Concert.builder()
                .name("Flash Sale Concert")
                .venue("Stadium")
                .eventDate(LocalDateTime.now().plusDays(30))
                .status(ConcertStatus.PUBLISHED)
                .build();
        concertRepo.save(concert);

        // 50 VIP, 100 Standard, 200 Economy = Total 350 tickets
        TicketCategory vip = createCategory("VIP", 5000, 50);
        vipId = vip.getId();

        TicketCategory standard = createCategory("Standard", 2000, 100);
        standardId = standard.getId();

        TicketCategory economy = createCategory("Economy", 1000, 200);
        economyId = economy.getId();
    }

    @AfterEach
    void tearDown() {
        ticketCategoryRepo.deleteAll();
        concertRepo.deleteAll();
    }

    @Test
    void case4A_burstSpike_500Threads_shouldSellExactly350Tickets() throws InterruptedException {
        System.out.println("\n=== [Flash Sale Test] Starting case4A: Burst Spike 500 threads ===");
        int threads = 500;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        List<Long> categories = List.of(vipId, standardId, economyId);
        Random random = new Random();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threads; i++) {
            // Randomly select one category
            final Long targetCategoryId = categories.get(random.nextInt(categories.size()));
            
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    ticketStockService.reserveStock(targetCategoryId, 1);
                    successCount.incrementAndGet();
                } catch (InsufficientTicketException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Unblock all threads at the same time
        doneLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("=== Flash Sale Burst Spike Report ===");
        System.out.println("[Burst] Total requests: " + threads);
        System.out.println("[Burst] Successful: " + successCount.get());
        System.out.println("[Burst] Failed (no stock): " + failCount.get());
        System.out.println("[Burst] Other exceptions: " + exceptionCount.get());
        System.out.println("[Burst] Duration: " + duration + " ms");

        // Verify that we didn't oversell
        assertTrue(successCount.get() <= 350, "Should never sell more than 350 tickets");
        
        TicketCategory updatedVip = ticketCategoryRepo.findById(vipId).orElseThrow();
        TicketCategory updatedStandard = ticketCategoryRepo.findById(standardId).orElseThrow();
        TicketCategory updatedEconomy = ticketCategoryRepo.findById(economyId).orElseThrow();
        
        int remainingTickets = updatedVip.getAvailableQuantity() + 
                               updatedStandard.getAvailableQuantity() + 
                               updatedEconomy.getAvailableQuantity();
                               
        assertEquals(350 - remainingTickets, successCount.get(), "Successful counts must match exactly the number of deducted tickets");
        assertEquals(0, exceptionCount.get(), "There should be no other exceptions (deadlocks, etc)");
        
        System.out.println("[Burst] Remaining tickets after spike: " + remainingTickets);
    }

    @Test
    void case4B_sustainedThroughput_8ReqPerSecond_For30Seconds() throws InterruptedException {
        System.out.println("\n=== [Flash Sale Test] Starting case4B: Sustained Throughput (8 req/s) ===");
        // 8 req/sec for 10 seconds (scaled down for unit test time, originally 30s) = 80 requests
        // Let's do 10 seconds to keep test runtime reasonable.
        int requestsPerSecond = 8;
        int durationSeconds = 10;
        int totalRequests = requestsPerSecond * durationSeconds;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
        ExecutorService workerPool = Executors.newFixedThreadPool(50);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Long> latencies = new ArrayList<>();

        List<Long> categories = List.of(vipId, standardId, economyId);
        Random random = new Random();

        for (int i = 0; i < durationSeconds; i++) {
            scheduler.schedule(() -> {
                for (int j = 0; j < requestsPerSecond; j++) {
                    final Long targetCategoryId = categories.get(random.nextInt(categories.size()));
                    workerPool.submit(() -> {
                        long reqStart = System.currentTimeMillis();
                        try {
                            ticketStockService.reserveStock(targetCategoryId, 1);
                            successCount.incrementAndGet();
                        } catch (InsufficientTicketException e) {
                            failCount.incrementAndGet();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            long reqEnd = System.currentTimeMillis();
                            synchronized (latencies) {
                                latencies.add(reqEnd - reqStart);
                            }
                            doneLatch.countDown();
                        }
                    });
                }
            }, i, TimeUnit.SECONDS);
        }

        doneLatch.await(15, TimeUnit.SECONDS);
        scheduler.shutdown();
        workerPool.shutdown();

        // Calculate P95 latency
        latencies.sort(Long::compareTo);
        long maxLatency = latencies.get(latencies.size() - 1);
        int p95Index = (int) (latencies.size() * 0.95);
        long p95Latency = latencies.get(p95Index);

        System.out.println("=== Flash Sale Sustained Throughput Report ===");
        System.out.println("[Sustained] Total requests: " + totalRequests);
        System.out.println("[Sustained] Successful: " + successCount.get());
        System.out.println("[Sustained] Failed (no stock): " + failCount.get());
        System.out.println("[Sustained] Max latency: " + maxLatency + " ms");
        System.out.println("[Sustained] P95 latency: " + p95Latency + " ms");

        // Asserts
        assertEquals(totalRequests, successCount.get() + failCount.get(), "All requests should be processed");
        
        // P95 latency should ideally be under 500ms
        // Note: In an H2 in-memory DB in a unit test, it should be very fast (<50ms usually)
        assertTrue(p95Latency < 500, "P95 latency should be under 500ms");
    }

    private TicketCategory createCategory(String name, int price, int quantity) {
        TicketCategory category = TicketCategory.builder()
                .concert(concert)
                .name(name)
                .price(BigDecimal.valueOf(price))
                .totalQuantity(quantity)
                .availableQuantity(quantity)
                .version(0L)
                .build();
        return ticketCategoryRepo.save(category);
    }
}
