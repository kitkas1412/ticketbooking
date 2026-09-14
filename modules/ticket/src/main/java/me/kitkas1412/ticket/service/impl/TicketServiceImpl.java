package me.kitkas1412.ticket.service.impl;

import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.common.exception.NoTicketAvailableException;
import me.kitkas1412.ticket.repository.TicketRepository;
import me.kitkas1412.ticket.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Lấy một vé AVAILABLE rồi chuyển sang SOLD trong transaction DB.
 */
@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    public TicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    @Transactional
    public Ticket reserveTicket(UUID eventId) {
        // Query chưa dùng pessimistic locking; @Version trên Ticket cung cấp optimistic locking.
        Ticket ticket = ticketRepository.findFirstByEventAndStatus(eventId, Ticket.TicketStatus.AVAILABLE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Ticket"));
        ticket.setStatus(Ticket.TicketStatus.SOLD);
        return ticketRepository.save(ticket);
    }
}
