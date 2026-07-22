package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import com.example.concert_ticket_booking_platform.Entity.TicketHold;
import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.Entity.enums.HoldStatus;
import com.example.concert_ticket_booking_platform.Repository.BookingRepo;
import com.example.concert_ticket_booking_platform.Repository.TicketHoldRepo;
import com.example.concert_ticket_booking_platform.dto.booking.BookingResponse;
import com.example.concert_ticket_booking_platform.dto.hold.ConfirmHoldRequest;
import com.example.concert_ticket_booking_platform.dto.hold.CreateHoldRequest;
import com.example.concert_ticket_booking_platform.dto.hold.HoldResponse;
import com.example.concert_ticket_booking_platform.exception.hold.HoldConflictException;
import com.example.concert_ticket_booking_platform.exception.hold.HoldExpiredException;
import com.example.concert_ticket_booking_platform.exception.hold.HoldNotFoundException;
import com.example.concert_ticket_booking_platform.exception.hold.HoldOwnershipException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HoldService {

    // Ngắn hơn nhiều so với BOOKING_HOLD_MINUTES (15p) ở BookingService, vì đây chỉ là bước
    // "khoá vé trong lúc user thao tác trên UI" trước khi thật sự tạo booking + payment.
    private static final int HOLD_MINUTES = 5;

    private final TicketHoldRepo ticketHoldRepository;
    private final TicketStockService ticketStockService;
    private final BookingService bookingService;
    private final BookingRepo bookingRepository;

    // -----------------------------------------------------------------
    // 1) Tạo hold — trừ kho ngay, chưa tạo Booking
    // -----------------------------------------------------------------
    @Transactional
    public HoldResponse createHold(User currentUser, String idempotencyKey, CreateHoldRequest request) {

        Optional<TicketHold> existingOpt = ticketHoldRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            return handleExistingHold(existingOpt.get(), request);
        }

        TicketCategory ticketCategory = ticketStockService.reserveStock(
                request.getTicketCategoryId(), request.getQuantity());

        TicketHold hold = TicketHold.builder()
                .user(currentUser)
                .ticketCategory(ticketCategory)
                .quantity(request.getQuantity())
                .status(HoldStatus.HELD)
                .idempotencyKey(idempotencyKey)
                .expiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES))
                .build();

        TicketHold saved;
        try {
            saved = ticketHoldRepository.save(hold);
        } catch (DataIntegrityViolationException e) {
            // Race hiếm gặp: 2 request cùng Idempotency-Key tới gần như đồng thời.
            // Request này "thua" nên phải hoàn lại phần kho vừa trừ ở trên trước khi trả về hold của người thắng.
            ticketStockService.releaseStock(ticketCategory.getId(), request.getQuantity());
            TicketHold winner = ticketHoldRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e);
            return handleExistingHold(winner, request);
        }

        return mapToResponse(saved, false);
    }

    private HoldResponse handleExistingHold(TicketHold existing, CreateHoldRequest request) {
        boolean samePayload = Objects.equals(existing.getTicketCategory().getId(), request.getTicketCategoryId())
                && Objects.equals(existing.getQuantity(), request.getQuantity());
        if (!samePayload) {
            throw new HoldConflictException(
                    "Idempotency-Key đã được dùng cho một request khác với dữ liệu khác");
        }
        return mapToResponse(existing, true);
    }

    // -----------------------------------------------------------------
    // 2) Confirm hold -> tạo Booking + Payment thật (dùng lại BookingService)
    // -----------------------------------------------------------------
    @Transactional
    public BookingResponse confirmHold(User currentUser, Long holdId, ConfirmHoldRequest request) {
        TicketHold hold = ticketHoldRepository.findById(holdId)
                .orElseThrow(() -> new HoldNotFoundException("Không tìm thấy lượt giữ chỗ"));

        if (!hold.getUser().getId().equals(currentUser.getId())) {
            throw new HoldOwnershipException("Bạn không có quyền xác nhận lượt giữ chỗ này");
        }

        // Idempotent: confirm nhiều lần trên 1 hold đã CONFIRMED -> trả về đúng booking cũ,
        // không xử lý lại nghiệp vụ (không trừ kho/áp voucher thêm lần nữa).
        if (hold.getStatus() == HoldStatus.CONFIRMED) {
            return bookingService.getBookingResponse(hold.getBooking().getId(), currentUser);
        }

        if (hold.getStatus() != HoldStatus.HELD) {
            throw new HoldExpiredException("Lượt giữ chỗ đã hết hạn hoặc không còn hiệu lực");
        }

        if (hold.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Hết hạn nhưng HoldExpiryScheduler chưa kịp quét — tự xử lý ngay tại đây
            // để không confirm nhầm 1 hold đáng lẽ đã mất hiệu lực.
            ticketStockService.releaseStock(hold.getTicketCategory().getId(), hold.getQuantity());
            hold.setStatus(HoldStatus.EXPIRED);
            ticketHoldRepository.save(hold);
            throw new HoldExpiredException("Lượt giữ chỗ đã hết hạn");
        }

        // idempotencyKey của Booking được sinh từ chính holdId — đảm bảo confirm là idempotent
        // theo bản chất (unique theo hold), không cần client tự gửi thêm 1 key khác ở bước này.
        String bookingIdempotencyKey = "hold-" + hold.getId();

        BookingResponse response = bookingService.buildBookingFromReservedStock(
                currentUser, hold.getTicketCategory(), hold.getQuantity(),
                bookingIdempotencyKey, normalizeVoucherCode(request.getVoucherCode()));

        // getReferenceById chỉ tạo 1 proxy trỏ tới id đã biết (response.getId()), không cần
        // SELECT lại toàn bộ Booking — rẻ hơn nhiều so với bookingRepository.findById(...).
        hold.setStatus(HoldStatus.CONFIRMED);
        hold.setBooking(bookingRepository.getReferenceById(response.getId()));
        ticketHoldRepository.save(hold);

        return response;
    }

    // -----------------------------------------------------------------
    // 3) Huỷ hold thủ công (user đổi ý trước khi confirm)
    // -----------------------------------------------------------------
    @Transactional
    public void releaseHold(User currentUser, Long holdId) {
        TicketHold hold = ticketHoldRepository.findById(holdId)
                .orElseThrow(() -> new HoldNotFoundException("Không tìm thấy lượt giữ chỗ"));

        if (!hold.getUser().getId().equals(currentUser.getId())) {
            throw new HoldOwnershipException("Bạn không có quyền huỷ lượt giữ chỗ này");
        }

        if (hold.getStatus() != HoldStatus.HELD) {
            return; // đã CONFIRMED/EXPIRED/RELEASED từ trước — coi như thao tác idempotent, không lỗi
        }

        ticketStockService.releaseStock(hold.getTicketCategory().getId(), hold.getQuantity());
        hold.setStatus(HoldStatus.RELEASED);
        ticketHoldRepository.save(hold);
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private String normalizeVoucherCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private HoldResponse mapToResponse(TicketHold hold, boolean isReplay) {
        return HoldResponse.builder()
                .id(hold.getId())
                .ticketCategoryId(hold.getTicketCategory().getId())
                .ticketCategoryName(hold.getTicketCategory().getName())
                .quantity(hold.getQuantity())
                .status(hold.getStatus())
                .expiresAt(hold.getExpiresAt())
                .createdAt(hold.getCreatedAt())
                .bookingId(hold.getBooking() != null ? hold.getBooking().getId() : null)
                .isReplay(isReplay)
                .build();
    }
}
