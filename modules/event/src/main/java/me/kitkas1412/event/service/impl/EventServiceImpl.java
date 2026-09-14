package me.kitkas1412.event.service.impl;

import me.kitkas1412.cache.TicketInventoryKey;
import me.kitkas1412.common.event.TicketRequestedEvent;
import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.event.dto.request.CreateEventRequest;
import me.kitkas1412.event.dto.request.UpdateEventRequest;
import me.kitkas1412.event.dto.response.EventResponse;
import me.kitkas1412.event.entity.Event;
import me.kitkas1412.event.mapper.EventMapper;
import me.kitkas1412.event.repository.EventRepository;
import me.kitkas1412.event.service.EventService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Lưu sự kiện, phát yêu cầu tạo vé và khởi tạo bộ đếm tồn kho Redis.
 */
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

        // Listener chạy đồng bộ, tạo vé trong cùng transaction với sự kiện.
        eventPublisher.publishEvent(new TicketRequestedEvent(
                this, event.getId(), request.totalTickets(), request.ticketPrice()));

        // Khởi tạo tồn kho Redis; thao tác này không tự rollback cùng JPA transaction.
        redisTemplate.opsForValue().set(TicketInventoryKey.availableTickets(event.getId()), String.valueOf(request.totalTickets()));
        System.out.println(redisTemplate.opsForValue().get(TicketInventoryKey.availableTickets(event.getId())));

        return eventMapper.toResponse(event);
    }

    @Override
    public EventResponse getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy event"));

        return eventMapper.toResponse(event);
    }

    @Override
    public EventResponse updateEvent(UpdateEventRequest updateEventRequest) {
        // Chưa triển khai nghiệp vụ cập nhật sự kiện.
        return null;
    }

    @Override
    // Chưa triển khai nghiệp vụ xóa sự kiện.
    public void deleteEvent(UUID eventId) {

    }
}
