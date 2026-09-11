package me.kitkas1412.ticket.service;

import me.kitkas1412.event.entity.Event;
import me.kitkas1412.ticket.entity.Ticket;

public interface TicketService {
    Ticket reserveTicket(Event event);
}
