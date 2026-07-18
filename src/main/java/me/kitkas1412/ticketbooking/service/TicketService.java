package me.kitkas1412.ticketbooking.service;

import me.kitkas1412.ticketbooking.dto.request.BuyTicketRequest;
import me.kitkas1412.ticketbooking.dto.response.TicketResponse;
import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Order;
import me.kitkas1412.ticketbooking.entity.Ticket;
import me.kitkas1412.ticketbooking.exception.EventNotFoundException;
import me.kitkas1412.ticketbooking.exception.NoTicketAvailableException;
import me.kitkas1412.ticketbooking.mapper.TicketMapper;
import me.kitkas1412.ticketbooking.repository.EventRepository;
import me.kitkas1412.ticketbooking.repository.OrderRepository;
import me.kitkas1412.ticketbooking.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TicketService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
//    private final TicketMapper ticketMapper;

    public TicketService(EventRepository eventRepository, TicketRepository ticketRepository, OrderRepository orderRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
//        this.ticketMapper = ticketMapper;
    }

    @Transactional
    public TicketResponse buyTicket(BuyTicketRequest request, UUID eventId){
        Event event = findEventByEventId(eventId);
        Ticket ticket = findAvailableTicketOrThrow(event);

        ticket.setStatus(Ticket.TicketStatus.SOLD);

        ticketRepository.save(ticket);

        Order order = orderRepository.save(Order.builder().idempotencyKey(request.idempotencyKey()).ticket(ticket).build());

//        return ticketMapper.toResponse(ticket);

        return new TicketResponse(ticket.getTicketId(), ticket.getStatus(), ticket.getEvent().getEventId(), order.getOrderId());
    }

    private Ticket findAvailableTicketOrThrow(Event event){
        return ticketRepository.findFirstByEventAndStatus(event, Ticket.TicketStatus.AVAILABLE)
                .orElseThrow(() -> new NoTicketAvailableException("Hết vé!"));
    }

    private Event findEventByEventId(UUID eventId){
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Không tìm thấy Event!"));
    }
}
