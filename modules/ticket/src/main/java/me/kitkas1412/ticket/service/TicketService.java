package me.kitkas1412.ticket.service;

import me.kitkas1412.ticket.entity.Ticket;

import java.util.UUID;

public interface TicketService {
    Ticket reserveTicket(UUID eventId);
}
