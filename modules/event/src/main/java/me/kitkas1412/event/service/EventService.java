package me.kitkas1412.event.service;

import me.kitkas1412.event.dto.request.CreateEventRequest;
import me.kitkas1412.event.dto.response.EventResponse;

public interface EventService {
    EventResponse createEvent(CreateEventRequest eventRequest);
}
