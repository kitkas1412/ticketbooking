package me.kitkas1412.ticket.repository;

import me.kitkas1412.event.entity.Event;
import me.kitkas1412.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findFirstByEventAndStatus(Event event, Ticket.TicketStatus ticketStatus);

    Long countByEventAndStatus(Event event, Ticket.TicketStatus status);
}
