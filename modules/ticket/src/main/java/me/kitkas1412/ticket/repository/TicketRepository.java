package me.kitkas1412.ticket.repository;

import me.kitkas1412.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findFirstByEventAndStatus(UUID eventId, Ticket.TicketStatus ticketStatus);

    Long countByEventAndStatus(UUID eventId, Ticket.TicketStatus status);
}
