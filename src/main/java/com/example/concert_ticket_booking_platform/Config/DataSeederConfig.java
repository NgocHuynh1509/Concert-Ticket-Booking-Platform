package com.example.concert_ticket_booking_platform.Config;

import com.example.concert_ticket_booking_platform.Entity.BaseEntity;
import com.example.concert_ticket_booking_platform.Entity.Concert;
import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Entity.Voucher;
import com.example.concert_ticket_booking_platform.Entity.enums.ConcertStatus;
import com.example.concert_ticket_booking_platform.Entity.enums.DiscountType;
import com.example.concert_ticket_booking_platform.Entity.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Configuration
public class DataSeederConfig {

    private static final String DUMMY_HASH = "$2a$12$R9h/cIPz0gi.URNNX3rub2Da9rRVmxbW.s.x/sH.N/9zV2a.N.M2m";

    @Bean
    public CommandLineRunner initDatabase(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        return args -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LocalDateTime now = LocalDateTime.now();

            if (count(entityManager, "User") == 0) {
                seedUsers(entityManager, now);
                System.out.println("Seeded users.");
            }

            if (count(entityManager, "Voucher") == 0) {
                seedVouchers(entityManager, now);
                System.out.println("Seeded vouchers.");
            }

            if (count(entityManager, "Concert") == 0) {
                seedConcerts(entityManager, now);
                System.out.println("Seeded concerts and ticket categories.");
            }

            System.out.println("Seeding completed successfully.");
        });
    }

    private void seedUsers(EntityManager entityManager, LocalDateTime now) {
        List<User> users = Arrays.asList(
                touch(User.builder().username("customer1").email("customer1@gmail.com").passwordHash(DUMMY_HASH).role(UserRole.CUSTOMER).build(), now),
                touch(User.builder().username("customer2").email("customer2@gmail.com").passwordHash(DUMMY_HASH).role(UserRole.CUSTOMER).build(), now),
                touch(User.builder().username("operator_nv").email("operator@geekup.vn").passwordHash(DUMMY_HASH).role(UserRole.OPERATOR).build(), now),
                touch(User.builder().username("admin_root").email("admin@geekup.vn").passwordHash(DUMMY_HASH).role(UserRole.ADMIN).build(), now)
        );

        users.forEach(entityManager::persist);
    }

    private void seedVouchers(EntityManager entityManager, LocalDateTime now) {
        List<Voucher> vouchers = Arrays.asList(
                touch(createVoucher("EARLYBIRD20", DiscountType.PERCENTAGE, "20", 500, 1, now.minusDays(1), now.plusDays(30)), now),
                touch(createVoucher("FLASH500K", DiscountType.FIXED_AMOUNT, "500000", 100, 1, now, now.plusDays(3)), now),
                touch(createVoucher("WELCOME10", DiscountType.PERCENTAGE, "10", 1000, 2, now.minusDays(5), now.plusMonths(6)), now),
                touch(createVoucher("VIPONLY1M", DiscountType.FIXED_AMOUNT, "1000000", 50, 1, now, now.plusDays(7)), now),
                touch(createVoucher("GEEKUP26", DiscountType.PERCENTAGE, "26", 200, 1, now.minusDays(2), now.plusDays(14)), now),
                touch(createVoucher("NIGHTOWL", DiscountType.FIXED_AMOUNT, "200000", 300, 3, now, now.plusDays(10)), now),
                touch(createVoucher("STUDENT15", DiscountType.PERCENTAGE, "15", 5000, 2, now.minusMonths(1), now.plusMonths(5)), now),
                touch(createVoucher("SUMMERVIBE", DiscountType.FIXED_AMOUNT, "150000", 800, 1, now, now.plusMonths(1)), now),
                touch(createVoucher("WEEKEND", DiscountType.PERCENTAGE, "5", 2000, 5, now, now.plusDays(2)), now),
                touch(createVoucher("BLACKFRIDAY", DiscountType.PERCENTAGE, "50", 10, 1, now.plusDays(10), now.plusDays(11)), now)
        );

        vouchers.forEach(entityManager::persist);
    }

    private void seedConcerts(EntityManager entityManager, LocalDateTime now) {
        List<String> concertNames = Arrays.asList(
                "Sơn Tùng M-TP: Sky Tour 2026", "Hà Anh Tuấn Live Concert: Chân Trời Rực Rỡ",
                "Đen Vâu: Show Của Đen", "Anh Trai Say Hi: The Final Concert",
                "Rap Việt All-Star Live Concert", "SpaceSpeakers: Kosmik 2",
                "Vũ Cát Tường: Inner Me 2", "Hoàng Thuỳ Linh: Vietnamese Concert",
                "Ngọt Band: Kỷ Niệm 10 Năm", "Chillies: Trên Những Đám Mây",
                "BlackPink: Born Pink (Encore HCMC)", "Westlife: The Hits Tour VN",
                "Charlie Puth Live in Vietnam", "Maroon 5: Asia Tour - HCMC",
                "Monsoon Music Festival 2026", "Lễ hội Âm nhạc Quốc tế HOZO",
                "EDM Festival: Ravolution", "Indie Mùa Thu Concert",
                "Symphony of the Night: Vietnam National Symphony", "T-Ara Fanmeeting & Mini Concert"
        );

        List<String> venues = Arrays.asList(
                "Sân vận động Mỹ Đình, Hà Nội", "Sân vận động Quân khu 7, TP.HCM",
                "Sân vận động Thống Nhất, TP.HCM", "Nhà thi đấu Phú Thọ, TP.HCM",
                "Cung Thể thao Quần Ngựa, Hà Nội", "SECC Quận 7, TP.HCM",
                "Trung tâm Hội nghị Quốc gia, Hà Nội", "Nhà hát Hòa Bình, TP.HCM"
        );

        Random random = new Random(42L);

        for (int i = 0; i < concertNames.size(); i++) {
            Concert concert = touch(Concert.builder()
                    .name(concertNames.get(i))
                    .description("Siêu đại nhạc hội " + concertNames.get(i) + " với những màn trình diễn đỉnh cao và dàn âm thanh ánh sáng đạt chuẩn quốc tế.")
                    .venue(venues.get(random.nextInt(venues.size())))
                    .eventDate(now.plusDays(10 + random.nextInt(170)))
                    .status(i < 17 ? ConcertStatus.PUBLISHED : ConcertStatus.DRAFT)
                    .concertMapUrl("https://via.placeholder.com/800x600.png?text=Seat+Map+" + (i + 1))
                    .posterUrl("https://via.placeholder.com/600x800.png?text=Poster+" + (i + 1))
                    .ticketCategories(new ArrayList<>())
                    .build(), now);

            generateTicketCategories(concert, 3 + random.nextInt(3), now);
            entityManager.persist(concert);
        }
    }

    private Voucher createVoucher(String code, DiscountType type, String value, int maxUsage, int perUserLimit, LocalDateTime from, LocalDateTime to) {
        return Voucher.builder()
                .code(code)
                .discountType(type)
                .discountValue(new BigDecimal(value))
                .maxUsage(maxUsage)
                .perUserLimit(perUserLimit)
                .validFrom(from)
                .validTo(to)
                .usedCount(0)
                .active(true)
                .version(0L)
                .build();
    }

    private void generateTicketCategories(Concert concert, int numCategories, LocalDateTime now) {
        String[] tierNames = {"GA (Đứng)", "Standard (Ngồi)", "Premium", "VIP", "VVIP"};
        String[] tierPrices = {"500000", "800000", "1500000", "3000000", "5000000"};
        int[] tierQuantities = {2000, 1500, 800, 300, 100};

        for (int i = 0; i < numCategories; i++) {
            TicketCategory category = touch(TicketCategory.builder()
                    .concert(concert)
                    .name(tierNames[i])
                    .price(new BigDecimal(tierPrices[i]))
                    .totalQuantity(tierQuantities[i])
                    .availableQuantity(tierQuantities[i])
                    .version(0L)
                    .build(), now);

            concert.getTicketCategories().add(category);
        }
    }

    private long count(EntityManager entityManager, String entityName) {
        return entityManager.createQuery("select count(e) from " + entityName + " e", Long.class).getSingleResult();
    }

    private <T extends BaseEntity> T touch(T entity, LocalDateTime timestamp) {
        entity.setCreatedAt(timestamp);
        entity.setUpdatedAt(timestamp);
        return entity;
    }
}