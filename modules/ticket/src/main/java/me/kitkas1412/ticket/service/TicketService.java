package me.kitkas1412.ticket.service;

import me.kitkas1412.ticket.entity.Ticket;

import java.util.UUID;

/**
 * Interface cấp một vé AVAILABLE của sự kiện khi xử lý đơn mua vé.
 */
public interface TicketService {
    Ticket reserveTicket(UUID eventId);
}
