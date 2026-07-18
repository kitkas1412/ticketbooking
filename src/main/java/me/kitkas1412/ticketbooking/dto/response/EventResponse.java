package me.kitkas1412.ticketbooking.dto.response;

import java.util.UUID;

public record EventResponse(UUID eventId, String name, Integer quantity) {
}
