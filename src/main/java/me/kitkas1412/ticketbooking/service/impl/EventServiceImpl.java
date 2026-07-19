package me.kitkas1412.ticketbooking.service.impl;

import me.kitkas1412.ticketbooking.dto.request.CreateEventRequest;
import me.kitkas1412.ticketbooking.dto.response.EventResponse;
import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Ticket;
import me.kitkas1412.ticketbooking.mapper.EventMapper;
import me.kitkas1412.ticketbooking.repository.EventRepository;
import me.kitkas1412.ticketbooking.repository.TicketRepository;
import me.kitkas1412.ticketbooking.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(EventRepository eventRepository, TicketRepository ticketRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public EventResponse createEvent(CreateEventRequest request){
        Event event = eventRepository.save(Event.builder().name(request.name()).description(request.description()).totalTickets(request.totalTickets()).saleStartAt(request.saleStartAt()).saleEndAt(request.saleEndAt()).build());

        List<Ticket> tickets = IntStream.rangeClosed(1, request.totalTickets())
                .mapToObj(seatCode -> buildTicket(event, seatCode, request.ticketPrice()))
                .toList();
        ticketRepository.saveAll(tickets);

        return eventMapper.toResponse(event);
    }

    private Ticket buildTicket(Event event, int seatCode, BigDecimal price) {
        return Ticket.builder().event(event).seatCode(seatCode).price(price).build();
    }
}
