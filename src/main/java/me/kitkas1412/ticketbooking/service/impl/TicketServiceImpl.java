package me.kitkas1412.ticketbooking.service.impl;

import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Ticket;
import me.kitkas1412.ticketbooking.exception.NoTicketAvailableException;
import me.kitkas1412.ticketbooking.repository.TicketRepository;
import me.kitkas1412.ticketbooking.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    public TicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    @Transactional
    public Ticket reserveTicket(Event event) {
        Ticket ticket = findAvailableTicketOrThrow(event);
        ticket.setStatus(Ticket.TicketStatus.SOLD);
        return ticketRepository.save(ticket);
    }

    private Ticket findAvailableTicketOrThrow(Event event) {
        return ticketRepository.findFirstByEventAndStatus(event, Ticket.TicketStatus.AVAILABLE)
                .orElseThrow(() -> new NoTicketAvailableException("Hết vé!"));
    }
}
