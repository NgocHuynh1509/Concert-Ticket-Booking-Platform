package com.example.concert_ticket_booking_platform;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling   // <-- THÊM DÒNG NÀY, thiếu nó thì mọi @Scheduled trong app không chạy
public class ConcertTicketBookingPlatformApplication {

    // 💡 Thêm đoạn này để ép cả hệ thống dùng múi giờ Việt Nam (GMT+7)
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ConcertTicketBookingPlatformApplication.class, args);
    }

}