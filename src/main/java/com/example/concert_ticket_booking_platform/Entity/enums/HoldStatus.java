package com.example.concert_ticket_booking_platform.Entity.enums;

/**
 * Vòng đời của 1 lượt giữ chỗ tạm (trước khi trở thành Booking thật).
 *
 *   HELD      -> vừa trừ kho, đang chờ user xác nhận (confirm) trong thời gian hold.
 *   CONFIRMED -> user đã confirm thành công, đã có Booking tương ứng.
 *   EXPIRED   -> quá hạn hold mà chưa confirm, kho vé đã được hoàn lại.
 *   RELEASED  -> user tự huỷ hold trước khi hết hạn, kho vé đã được hoàn lại.
 */
public enum HoldStatus {
    HELD,
    CONFIRMED,
    EXPIRED,
    RELEASED
}
