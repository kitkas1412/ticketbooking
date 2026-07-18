package me.kitkas1412.ticketbooking.dto.request;

import java.time.OffsetDateTime;

public record CreateEventRequest(
        String name,
        String description,
        Integer totalTickets,
        OffsetDateTime saleStartAt,
        OffsetDateTime saleEndAt) {
}
