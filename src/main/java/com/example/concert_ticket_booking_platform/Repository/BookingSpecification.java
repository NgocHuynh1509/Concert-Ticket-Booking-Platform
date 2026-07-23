package com.example.concert_ticket_booking_platform.Repository;


import com.example.concert_ticket_booking_platform.Entity.Booking;
import com.example.concert_ticket_booking_platform.Entity.BookingItem;
import com.example.concert_ticket_booking_platform.Entity.Payment;
import com.example.concert_ticket_booking_platform.Entity.TicketCategory;
import com.example.concert_ticket_booking_platform.Entity.User;
import com.example.concert_ticket_booking_platform.dto.booking.OperatorBookingFilterRequest;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BookingSpecification {

    public static Specification<Booking> fromFilter(OperatorBookingFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            Join<Booking, BookingItem> itemJoin = null;
            Join<BookingItem, TicketCategory> categoryJoin = null;

            if (filter.getConcertId() != null || filter.getTicketCategoryId() != null) {
                itemJoin = root.join("items", JoinType.INNER);
                categoryJoin = itemJoin.join("ticketCategory", JoinType.INNER);
            }

            if (filter.getConcertId() != null) {
                predicates.add(cb.equal(categoryJoin.get("concert").get("id"), filter.getConcertId()));
            }
            if (filter.getTicketCategoryId() != null) {
                predicates.add(cb.equal(categoryJoin.get("id"), filter.getTicketCategoryId()));
            }

            if (filter.getBookingStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getBookingStatus()));
            }

            if (filter.getPaymentStatus() != null) {
                Join<Booking, Payment> paymentJoin = root.join("payments", JoinType.INNER);
                predicates.add(cb.equal(paymentJoin.get("status"), filter.getPaymentStatus()));
            }

            if (filter.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
            }
            if (filter.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));
            }

            if (filter.getUserEmail() != null && !filter.getUserEmail().isBlank()) {
                Join<Booking, User> userJoin = root.join("user", JoinType.INNER);
                predicates.add(cb.like(cb.lower(userJoin.get("email")),
                        "%" + filter.getUserEmail().trim().toLowerCase() + "%"));
            }

            if (filter.getIdempotencyKey() != null && !filter.getIdempotencyKey().isBlank()) {
                predicates.add(cb.equal(root.get("idempotencyKey"), filter.getIdempotencyKey().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}