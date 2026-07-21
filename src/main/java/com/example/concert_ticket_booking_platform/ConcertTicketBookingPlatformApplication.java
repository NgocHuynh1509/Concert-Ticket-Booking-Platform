package com.example.concert_ticket_booking_platform;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
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