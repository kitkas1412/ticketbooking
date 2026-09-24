package me.kitkas1412.event.controller;

import me.kitkas1412.common.response.ApiResponse;
import me.kitkas1412.event.dto.request.CreateEventRequest;
import me.kitkas1412.event.dto.response.EventResponse;
import me.kitkas1412.event.entity.Event;
import me.kitkas1412.event.service.impl.EventServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventServiceImpl eventService;

    @InjectMocks
    private EventController controller;

    @Test
    void createEventReturns201WithCreatedEvent() {
        CreateEventRequest request = new CreateEventRequest(
                "Acoustic", "Nhà hát", 10, BigDecimal.TEN, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1));
        EventResponse created = eventResponse(UUID.randomUUID());
        when(eventService.createEvent(request)).thenReturn(created);

        ResponseEntity<ApiResponse<EventResponse>> response = controller.createEvent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(ApiResponse.success(created));
    }

    @Test
    void getEventReturnsEventWithDocumentedAcceptedStatus() {
        UUID eventId = UUID.randomUUID();
        EventResponse event = eventResponse(eventId);
        when(eventService.getEventById(eventId)).thenReturn(event);

        ResponseEntity<ApiResponse<EventResponse>> response = controller.getEvent(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(ApiResponse.success(event));
    }

    private static EventResponse eventResponse(UUID id) {
        return new EventResponse(id, "Acoustic", "Nhà hát", 10, null, null, Event.EventStatus.DRAFT);
    }
}
