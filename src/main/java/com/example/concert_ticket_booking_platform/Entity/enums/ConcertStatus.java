package com.example.concert_ticket_booking_platform.Entity.enums;

/**
 * Vòng đời của một concert.
 * DRAFT      -> operator đang chuẩn bị, chưa hiển thị cho customer
 * PUBLISHED  -> customer có thể browse & đặt vé
 * CANCELLED  -> concert bị huỷ, mọi booking liên quan phải được xử lý hoàn tiền (ngoài scope test này)
 * COMPLETED  -> concert đã diễn ra xong, chỉ để lưu trữ
 */
public enum ConcertStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    COMPLETED
}

