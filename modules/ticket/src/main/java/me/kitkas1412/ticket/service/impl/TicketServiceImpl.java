package me.kitkas1412.ticket.service.impl;

import me.kitkas1412.event.entity.Event;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.ticket.exception.NoTicketAvailableException;
import me.kitkas1412.ticket.repository.TicketRepository;
import me.kitkas1412.ticket.service.TicketService;
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
