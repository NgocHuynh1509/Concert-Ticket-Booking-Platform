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


    private static final int HOLD_MINUTES = 5;

    private final TicketHoldRepo ticketHoldRepository;
    private final TicketStockService ticketStockService;
    private final BookingService bookingService;
    private final BookingRepo bookingRepository;


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


    @Transactional
    public BookingResponse confirmHold(User currentUser, Long holdId, ConfirmHoldRequest request) {
        TicketHold hold = ticketHoldRepository.findById(holdId)
                .orElseThrow(() -> new HoldNotFoundException("Không tìm thấy lượt giữ chỗ"));

        if (!hold.getUser().getId().equals(currentUser.getId())) {
            throw new HoldOwnershipException("Bạn không có quyền xác nhận lượt giữ chỗ này");
        }


        if (hold.getStatus() == HoldStatus.CONFIRMED) {
            return bookingService.getBookingResponse(hold.getBooking().getId(), currentUser);
        }

        if (hold.getStatus() != HoldStatus.HELD) {
            throw new HoldExpiredException("Lượt giữ chỗ đã hết hạn hoặc không còn hiệu lực");
        }

        if (hold.getExpiresAt().isBefore(LocalDateTime.now())) {

            ticketStockService.releaseStock(hold.getTicketCategory().getId(), hold.getQuantity());
            hold.setStatus(HoldStatus.EXPIRED);
            ticketHoldRepository.save(hold);
            throw new HoldExpiredException("Lượt giữ chỗ đã hết hạn");
        }


        String bookingIdempotencyKey = "hold-" + hold.getId();

        BookingResponse response = bookingService.buildBookingFromReservedStock(
                currentUser, hold.getTicketCategory(), hold.getQuantity(),
                bookingIdempotencyKey, normalizeVoucherCode(request.getVoucherCode()));


        hold.setStatus(HoldStatus.CONFIRMED);
        hold.setBooking(bookingRepository.getReferenceById(response.getId()));
        ticketHoldRepository.save(hold);

        return response;
    }

    @Transactional
    public void releaseHold(User currentUser, Long holdId) {
        TicketHold hold = ticketHoldRepository.findById(holdId)
                .orElseThrow(() -> new HoldNotFoundException("Không tìm thấy lượt giữ chỗ"));

        if (!hold.getUser().getId().equals(currentUser.getId())) {
            throw new HoldOwnershipException("Bạn không có quyền huỷ lượt giữ chỗ này");
        }

        if (hold.getStatus() != HoldStatus.HELD) {
            return;
        }

        ticketStockService.releaseStock(hold.getTicketCategory().getId(), hold.getQuantity());
        hold.setStatus(HoldStatus.RELEASED);
        ticketHoldRepository.save(hold);
    }



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
