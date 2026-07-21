package com.example.concert_ticket_booking_platform.Service;

import com.example.concert_ticket_booking_platform.Entity.Concert;
import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import com.example.concert_ticket_booking_platform.Repository.ConcertRepo;
import com.example.concert_ticket_booking_platform.dto.concert.ConcertDetailDTO;
import com.example.concert_ticket_booking_platform.dto.concert.ConcertSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepo concertRepo;

    public List<ConcertSummaryResponse> getAllConcerts() {
        return concertRepo.findAll().stream()
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());
    }

    private ConcertSummaryResponse mapToSummaryResponse(Concert concert) {
        return ConcertSummaryResponse.builder()
                .id(concert.getId())
                .name(concert.getName())
                .venue(concert.getVenue())
                .eventDate(concert.getEventDate())
                .status(concert.getStatus())
                .posterUrl(concert.getPosterUrl())
                .build();
    }

    public ConcertDetailDTO getConcertDetail(Long id){
        Concert concert = concertRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Concert not found"));

        ConcertDetailDTO dto = new ConcertDetailDTO();

        dto.setId(concert.getId());
        dto.setName(concert.getName());
        dto.setDescription(concert.getDescription());
        dto.setVenue(concert.getVenue());
        dto.setEventDate(concert.getEventDate());
        dto.setPosterUrl(concert.getPosterUrl());
        dto.setConcertMapUrl(concert.getConcertMapUrl());
        dto.setStatus(concert.getStatus().name());

        List<ConcertDetailDTO.TicketDTO> tickets = new ArrayList<>();

        for(TicketCategory ticket : concert.getTicketCategories()){

            ConcertDetailDTO.TicketDTO t = new ConcertDetailDTO.TicketDTO();

            t.setId(ticket.getId());
            t.setName(ticket.getName());
            t.setPrice(ticket.getPrice());
            t.setAvailableQuantity(ticket.getAvailableQuantity());

            tickets.add(t);
        }

        dto.setTickets(tickets);

        return dto;
    }
}
