package me.kitkas1412.ticketbooking.dto.response;

import me.kitkas1412.ticketbooking.entity.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID eventId,
        String name,
        String description,
        Integer totalTickets,
        OffsetDateTime saleStartAt,
        OffsetDateTime saleEndAt,
        Event.EventStatus eventStatus) {
}
