package me.kitkas1412.ticketbooking.dto.response;

import me.kitkas1412.ticketbooking.entity.Ticket;

import java.math.BigDecimal;
import java.util.UUID;

public record BuyTicketResponse(
        UUID ticketId,
        Integer seatCode,
        Ticket.TicketStatus status,
        UUID eventId,
        UUID orderId,
        BigDecimal price) {
}
