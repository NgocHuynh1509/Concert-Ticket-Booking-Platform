package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.*;
import com.example.concert_ticket_booking_platform.Entity.enums.ConcertStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.UserRole;
import com.example.concert_ticket_booking_platform.Repository.*;
import com.example.concert_ticket_booking_platform.dto.booking.BookingResponse;
import com.example.concert_ticket_booking_platform.dto.booking.CreateBookingRequest;
import com.example.concert_ticket_booking_platform.exception.booking.IdempotencyConflictException;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class BookingIdempotencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepo bookingRepo;

    @Autowired
    private TicketCategoryRepo ticketCategoryRepo;

    @Autowired
    private ConcertRepo concertRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BookingItemRepo bookingItemRepo;

    @Autowired
    private PaymentRepo paymentRepo;

    private User user;
    private TicketCategory categoryA;
    private TicketCategory categoryB;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        paymentRepo.deleteAll();
        bookingItemRepo.deleteAll();
        bookingRepo.deleteAll();
        ticketCategoryRepo.deleteAll();
        concertRepo.deleteAll();
        userRepo.deleteAll();

        user = User.builder()
                .username("test_idemp")
                .email("test_idemp@gmail.com")
                .passwordHash("hash")
                .role(UserRole.CUSTOMER)
                .build();
        user = userRepo.save(user);

        Concert concert = Concert.builder()
                .name("Idempotency Concert")
                .venue("Test Venue")
                .eventDate(LocalDateTime.now().plusDays(10))
                .status(ConcertStatus.PUBLISHED)
                .build();
        concertRepo.save(concert);

        categoryA = TicketCategory.builder()
                .concert(concert)
                .name("VIP")
                .price(BigDecimal.valueOf(1000))
                .totalQuantity(100)
                .availableQuantity(100)
                .version(0L)
                .build();
        categoryA = ticketCategoryRepo.save(categoryA);

        categoryB = TicketCategory.builder()
                .concert(concert)
                .name("Standard")
                .price(BigDecimal.valueOf(500))
                .totalQuantity(100)
                .availableQuantity(100)
                .version(0L)
                .build();
        categoryB = ticketCategoryRepo.save(categoryB);

        idempotencyKey = UUID.randomUUID().toString();
    }

    @AfterEach
    void tearDown() {
        paymentRepo.deleteAll();
        bookingItemRepo.deleteAll();
        bookingRepo.deleteAll();
        ticketCategoryRepo.deleteAll();
        concertRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    void case2a_sameKeySamePayload_shouldReturnReplayAndOneBooking() {
        System.out.println("\n=== [Idempotency Test] Starting case2a: same key, same payload ===");
        CreateBookingRequest req = new CreateBookingRequest();
        req.setTicketCategoryId(categoryA.getId());
        req.setQuantity(2);

        // First call
        BookingResponse res1 = bookingService.createBooking(user, idempotencyKey, req);
        assertNotNull(res1.getId());
        assertFalse(res1.isReplay());

        // Second call with exact same request
        BookingResponse res2 = bookingService.createBooking(user, idempotencyKey, req);
        assertNotNull(res2.getId());
        assertTrue(res2.isReplay(), "Should be marked as replay");
        assertEquals(res1.getId(), res2.getId(), "Should return the same booking ID");

        long bookingCount = bookingRepo.count();
        System.out.println("[case2a] Finished. Total bookings in DB: " + bookingCount);
        assertEquals(1, bookingCount, "There should be exactly 1 booking in the database");
    }



    @Test
    void case2c_concurrentSameKeySamePayload_shouldCreateOnlyOneBooking() throws InterruptedException {
        System.out.println("\n=== [Idempotency Test] Starting case2c: concurrent requests with same key ===");
        int threads = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setTicketCategoryId(categoryA.getId());
        req.setQuantity(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    bookingService.createBooking(user, idempotencyKey, req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // 3 requests should all succeed (1 creates, 2 get replayed responses or handle gracefully)
        // Wait! createBooking doesn't throw Exception on concurrent if idempotency handles it via DataIntegrityViolationException.
        // It returns the existing booking. So successCount should be 3, exceptionCount 0.
        // And DB should have only 1 booking.
        
        long bookingCount = bookingRepo.count();
        System.out.println("[case2c] Finished. Success: " + successCount.get() + ", Exceptions: " + exceptionCount.get() + ", DB Bookings: " + bookingCount);
        assertEquals(1, bookingCount, "There should be exactly 1 booking despite concurrent requests");
    }


}
