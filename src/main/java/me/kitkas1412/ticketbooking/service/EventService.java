package me.kitkas1412.ticketbooking.service;

import me.kitkas1412.ticketbooking.dto.request.CreateEventRequest;
import me.kitkas1412.ticketbooking.dto.response.EventResponse;

public interface EventService {
    EventResponse createEvent(CreateEventRequest eventRequest);
}
