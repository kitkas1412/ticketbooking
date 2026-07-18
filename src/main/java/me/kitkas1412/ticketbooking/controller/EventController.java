package me.kitkas1412.ticketbooking.controller;

import me.kitkas1412.ticketbooking.dto.request.BuyTicketRequest;
import me.kitkas1412.ticketbooking.dto.request.CreateEventRequest;
import me.kitkas1412.ticketbooking.dto.response.EventResponse;
import me.kitkas1412.ticketbooking.dto.response.TicketResponse;
import me.kitkas1412.ticketbooking.service.EventService;
import me.kitkas1412.ticketbooking.service.EventServiceImpl;
import me.kitkas1412.ticketbooking.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventServiceImpl eventService;
    private final TicketService ticketService;

    public EventController(EventServiceImpl eventService, TicketService ticketService) {
        this.eventService = eventService;
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@RequestBody CreateEventRequest request){
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{eventId}/buy")
    public ResponseEntity<TicketResponse> buyTicket(@RequestBody BuyTicketRequest request, @PathVariable UUID eventId){
        TicketResponse response = ticketService.buyTicket(request, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
