package me.kitkas1412.event.service.impl;

import me.kitkas1412.common.cache.TicketInventoryKey;
import me.kitkas1412.common.event.TicketRequestedEvent;
import me.kitkas1412.event.dto.request.CreateEventRequest;
import me.kitkas1412.event.dto.response.EventResponse;
import me.kitkas1412.event.entity.Event;
import me.kitkas1412.event.mapper.EventMapper;
import me.kitkas1412.event.repository.EventRepository;
import me.kitkas1412.event.service.EventService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public EventServiceImpl(EventRepository eventRepository, EventMapper eventMapper, StringRedisTemplate redisTemplate, ApplicationEventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public EventResponse createEvent(CreateEventRequest request){
        Event event = eventRepository.save(Event.builder()
                .name(request.name())
                .description(request.description())
                .totalTickets(request.totalTickets())
                .saleStartAt(request.saleStartAt())
                .saleEndAt(request.saleEndAt())
                .build());

        eventPublisher.publishEvent(new TicketRequestedEvent(
                this, event.getId(), request.totalTickets(), request.ticketPrice()));

        redisTemplate.opsForValue().set(TicketInventoryKey.availableTickets(event.getId()), String.valueOf(request.totalTickets()));
        System.out.println(redisTemplate.opsForValue().get(TicketInventoryKey.availableTickets(event.getId())));

        return eventMapper.toResponse(event);
    }
}
