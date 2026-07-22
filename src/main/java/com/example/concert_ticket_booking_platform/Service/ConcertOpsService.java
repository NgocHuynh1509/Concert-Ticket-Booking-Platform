package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.Concert;
import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import com.example.concert_ticket_booking_platform.Entity.enums.ConcertStatus;
import com.example.concert_ticket_booking_platform.Repository.ConcertRepo;
import com.example.concert_ticket_booking_platform.Repository.TicketCategoryRepo;
import com.example.concert_ticket_booking_platform.dto.concert.TicketCategoryResponse;
import com.example.concert_ticket_booking_platform.dto.concert.ConcertOpsResponse;
import com.example.concert_ticket_booking_platform.dto.concert.ConcertSummaryResponse;
import com.example.concert_ticket_booking_platform.dto.concert.CreateConcertOpsRequest;
import com.example.concert_ticket_booking_platform.dto.concert.TicketCategoryCreateRequest;
import com.example.concert_ticket_booking_platform.dto.concert.UpdateConcertStatusRequest;
import com.example.concert_ticket_booking_platform.exception.InvalidStatusTransitionException;
import com.example.concert_ticket_booking_platform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Nghiep vu dashboard cho OPERATOR/ADMIN: tao concert (kem ticket category) va
 * chuyen trang thai concert theo dung vong doi mo ta trong ConcertStatus.
 */
@Service
@RequiredArgsConstructor
public class ConcertOpsService {

    private final ConcertRepo concertRepository;
    private final TicketCategoryRepo ticketCategoryRepos;

    /**
     * Ma tran chuyen trang thai hop le. Dung EnumMap/EnumSet thay vi if/else long
     * de de doc va de mo rong sau nay (vd them trang thai moi chi can sua 1 cho).
     *
     * DRAFT      -> PUBLISHED, CANCELLED
     * PUBLISHED  -> CANCELLED, COMPLETED
     * CANCELLED  -> (terminal - khong chuyen di dau nua)
     * COMPLETED  -> (terminal)
     */
    private static final Map<ConcertStatus, Set<ConcertStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ConcertStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ConcertStatus.DRAFT, EnumSet.of(ConcertStatus.PUBLISHED, ConcertStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(ConcertStatus.PUBLISHED, EnumSet.of(ConcertStatus.CANCELLED, ConcertStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(ConcertStatus.CANCELLED, EnumSet.noneOf(ConcertStatus.class));
        ALLOWED_TRANSITIONS.put(ConcertStatus.COMPLETED, EnumSet.noneOf(ConcertStatus.class));
    }

    /**
     * Dung rieng cho dashboard operator: tra ve TAT CA concert (moi status), khac voi
     * ConcertService.listPublishedConcerts() chi tra PUBLISHED cho khach vang lai.
     */
    @Transactional(readOnly = true)
    public List<ConcertSummaryResponse> listAllConcerts() {
        return concertRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId())) // moi tao len dau
                .map(c -> ConcertSummaryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .venue(c.getVenue())
                        .eventDate(c.getEventDate())
                        .status(c.getStatus())
                        .posterUrl(c.getPosterUrl())
                        .build())
                .toList();
    }

    @Transactional
    public ConcertOpsResponse createConcert(CreateConcertOpsRequest request) {
        // @Valid o Controller da chan cac truong bat buoc bi thieu (400), o day chi build entity.
        Concert concert = Concert.builder()
                .name(request.getName())
                .description(request.getDescription())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .concertMapUrl(request.getConcertMapUrl())
                .posterUrl(request.getPosterUrl())
                .status(ConcertStatus.DRAFT) // concert moi tao luon o DRAFT, chua hien thi cho customer
                .build();

        for (TicketCategoryCreateRequest tcReq : request.getTicketCategories()) {
            TicketCategory category = TicketCategory.builder()
                    .concert(concert)
                    .name(tcReq.getName())
                    .price(tcReq.getPrice())
                    .totalQuantity(tcReq.getTotalQuantity())
                    // availableQuantity khoi tao = totalQuantity, khong nhan tu client
                    .availableQuantity(tcReq.getTotalQuantity())
                    .build();
            concert.getTicketCategories().add(category);
        }

        Concert saved = concertRepository.save(concert);

        return ConcertOpsResponse.builder()
                .concertId(saved.getId())
                .status(saved.getStatus())
                .build();
    }

    @Transactional
    public ConcertOpsResponse updateStatus(Long concertId, UpdateConcertStatusRequest request) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay concert voi id: " + concertId));

        ConcertStatus current = concert.getStatus();
        ConcertStatus target = request.getStatus();

        if (current == target) {
            throw new InvalidStatusTransitionException(
                    "Concert dang o trang thai " + current + ", khong the chuyen sang chinh no");
        }

        Set<ConcertStatus> allowedNextStates = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowedNextStates.contains(target)) {
            throw new InvalidStatusTransitionException(
                    "Khong the chuyen concert tu " + current + " sang " + target +
                            ". Cac trang thai hop le tiep theo: " + allowedNextStates);
        }

        concert.setStatus(target);
        Concert saved = concertRepository.save(concert);

        return ConcertOpsResponse.builder()
                .concertId(saved.getId())
                .status(saved.getStatus())
                .build();
    }
    public List<TicketCategoryResponse> getTicketCategories(Long concertId, String name) {
        return ticketCategoryRepos.findByFilters(concertId, name).stream()
                .map(tc -> TicketCategoryResponse.builder()
                        .id(tc.getId())
                        .concertId(tc.getConcert().getId())
                        .concertName(tc.getConcert().getName())
                        .name(tc.getName())
                        .price(tc.getPrice())
                        .totalQuantity(tc.getTotalQuantity())
                        .availableQuantity(tc.getAvailableQuantity())
                        .soldCount(tc.getTotalQuantity() - tc.getAvailableQuantity())
                        .build())
                .toList();
    }
}
