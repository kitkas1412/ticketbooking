package me.kitkas1412.event.service;

import me.kitkas1412.event.dto.request.CreateEventRequest;
import me.kitkas1412.event.dto.request.UpdateEventRequest;
import me.kitkas1412.event.dto.response.EventResponse;

import java.util.UUID;

public interface EventService {
    EventResponse createEvent(CreateEventRequest eventRequest);

    EventResponse getEventById(UUID eventId);

    EventResponse updateEvent(UpdateEventRequest updateEventRequest);

    void deleteEvent(UUID eventId);
}
