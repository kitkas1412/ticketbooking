package me.kitkas1412.ticketbooking.service.impl;

import me.kitkas1412.ticketbooking.dto.request.BuyTicketRequest;
import me.kitkas1412.ticketbooking.dto.response.BuyTicketResponse;
import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Order;
import me.kitkas1412.ticketbooking.entity.Ticket;
import me.kitkas1412.ticketbooking.exception.EventNotFoundException;
import me.kitkas1412.ticketbooking.mapper.TicketMapper;
import me.kitkas1412.ticketbooking.repository.EventRepository;
import me.kitkas1412.ticketbooking.repository.OrderRepository;
import me.kitkas1412.ticketbooking.service.OrderItemService;
import me.kitkas1412.ticketbooking.service.OrderService;
import me.kitkas1412.ticketbooking.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketService ticketService;
    private final OrderItemService orderItemService;
    private final TicketMapper ticketMapper;

    public OrderServiceImpl(EventRepository eventRepository, OrderRepository orderRepository, TicketService ticketService, OrderItemService orderItemService, TicketMapper ticketMapper) {
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.ticketService = ticketService;
        this.orderItemService = orderItemService;
        this.ticketMapper = ticketMapper;
    }

    @Override
    @Transactional
    public BuyTicketResponse buyTicket(BuyTicketRequest request, UUID eventId) {
        Event event = findEventByIdOrThrow(eventId);
        Ticket ticket = ticketService.reserveTicket(event);

        Order order = orderRepository.save(Order.builder()
                .idempotencyKey(request.idempotencyKey())
                .event(event)
                .build());

        orderItemService.createOrderItem(order, ticket, ticket.getPrice());

        return ticketMapper.toBuyTicketResponse(ticket, order);
    }

    private Event findEventByIdOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Không tìm thấy Event!"));
    }
}
