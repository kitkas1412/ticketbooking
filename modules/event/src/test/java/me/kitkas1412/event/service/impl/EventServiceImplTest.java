package me.kitkas1412.event.service.impl;

import me.kitkas1412.cache.TicketInventoryKey;
import me.kitkas1412.common.event.TicketRequestedEvent;
import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.event.dto.request.CreateEventRequest;
import me.kitkas1412.event.dto.response.EventResponse;
import me.kitkas1412.event.entity.Event;
import me.kitkas1412.event.mapper.EventMapper;
import me.kitkas1412.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        eventService = new EventServiceImpl(eventRepository, eventMapper, redisTemplate, eventPublisher);
    }

    @Test
    void createEventPersistsEventRequestsTicketsAndSeedsRedisInventory() {
        UUID eventId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.parse("2026-09-01T09:00:00+07:00");
        OffsetDateTime end = OffsetDateTime.parse("2026-09-30T23:59:59+07:00");
        CreateEventRequest request = new CreateEventRequest(
                "Acoustic", "Nhà hát", 100, new BigDecimal("350000"), start, end);
        EventResponse expected = new EventResponse(eventId, "Acoustic", "Nhà hát", 100, start, end, Event.EventStatus.DRAFT);

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            saved.setId(eventId);
            return saved;
        });
        when(eventMapper.toResponse(any(Event.class))).thenReturn(expected);

        EventResponse response = eventService.createEvent(request);

        assertThat(response).isEqualTo(expected);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        Event saved = eventCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Acoustic");
        assertThat(saved.getDescription()).isEqualTo("Nhà hát");
        assertThat(saved.getTotalTickets()).isEqualTo(100);
        assertThat(saved.getSaleStartAt()).isEqualTo(start);
        assertThat(saved.getSaleEndAt()).isEqualTo(end);
        assertThat(saved.getStatus()).isEqualTo(Event.EventStatus.DRAFT);

        ArgumentCaptor<ApplicationEvent> published = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher).publishEvent(published.capture());
        assertThat(published.getValue()).isInstanceOfSatisfying(TicketRequestedEvent.class, ticketEvent -> {
            assertThat(ticketEvent.getEventId()).isEqualTo(eventId);
            assertThat(ticketEvent.getTotalTickets()).isEqualTo(100);
            assertThat(ticketEvent.getTicketPrice()).isEqualByComparingTo("350000");
        });

        verify(valueOperations).set(TicketInventoryKey.availableTickets(eventId), "100");
    }

    @Test
    void createEventRequestsTicketsBeforeSeedingRedis() {
        UUID eventId = UUID.randomUUID();
        CreateEventRequest request = new CreateEventRequest(
                "A", "B", 5, BigDecimal.TEN, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            saved.setId(eventId);
            return saved;
        });

        eventService.createEvent(request);

        InOrder order = inOrder(eventRepository, eventPublisher, valueOperations);
        order.verify(eventRepository).save(any(Event.class));
        order.verify(eventPublisher).publishEvent(any(ApplicationEvent.class));
        order.verify(valueOperations).set(TicketInventoryKey.availableTickets(eventId), "5");
    }

    @Test
    void getEventByIdReturnsMappedEvent() {
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder().name("Acoustic").build();
        EventResponse expected = new EventResponse(eventId, "Acoustic", null, null, null, null, Event.EventStatus.DRAFT);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expected);

        assertThat(eventService.getEventById(eventId)).isEqualTo(expected);
    }

    @Test
    void getEventByIdThrowsWhenMissing() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(eventId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy event");
    }
}
