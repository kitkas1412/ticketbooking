package me.kitkas1412.ticketbooking.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateEventRequest(
        String name,
        String description,
        Integer totalTickets,
        BigDecimal ticketPrice,
        OffsetDateTime saleStartAt,
        OffsetDateTime saleEndAt) {
}
