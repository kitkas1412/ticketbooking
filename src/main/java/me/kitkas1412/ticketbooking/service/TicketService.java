package me.kitkas1412.ticketbooking.service;

import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Ticket;

public interface TicketService {
    Ticket reserveTicket(Event event);
}
