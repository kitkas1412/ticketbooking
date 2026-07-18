package me.kitkas1412.ticketbooking.dto.response;

import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Order;
import me.kitkas1412.ticketbooking.entity.Ticket;

import java.util.UUID;

public record TicketResponse(UUID ticketId, Ticket.TicketStatus status, UUID eventId, UUID orderId) {
}
