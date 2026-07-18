package me.kitkas1412.ticketbooking.service;

import me.kitkas1412.ticketbooking.dto.request.CreateEventRequest;
import me.kitkas1412.ticketbooking.dto.response.EventResponse;
import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Ticket;
import me.kitkas1412.ticketbooking.mapper.EventMapper;
import me.kitkas1412.ticketbooking.repository.EventRepository;
import me.kitkas1412.ticketbooking.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
//    private final EventMapper eventMapper;

    public EventService(EventRepository eventRepository, TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
//        this.eventMapper = eventMapper;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest eventRequest){
        Event event = eventRepository.save(Event.builder().name(eventRequest.name()).quantity(eventRequest.quantity()).build());

        List<Ticket> ticketList = new ArrayList<>();

        for (int i = 0; i < eventRequest.quantity(); i++) {
            ticketList.add(Ticket.builder().event(event).build());
        }

        ticketRepository.saveAll(ticketList);

//        return eventMapper.toResponse(event);

        return new EventResponse(event.getEventId(), event.getName(), event.getQuantity());
    }
}
