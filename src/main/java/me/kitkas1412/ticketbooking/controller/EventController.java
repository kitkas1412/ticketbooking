package me.kitkas1412.ticketbooking.controller;

import me.kitkas1412.ticketbooking.dto.request.BuyTicketRequest;
import me.kitkas1412.ticketbooking.dto.request.CreateEventRequest;
import me.kitkas1412.ticketbooking.dto.response.ApiResponse;
import me.kitkas1412.ticketbooking.dto.response.BuyTicketResponse;
import me.kitkas1412.ticketbooking.dto.response.EventResponse;
import me.kitkas1412.ticketbooking.service.impl.EventServiceImpl;
import me.kitkas1412.ticketbooking.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventServiceImpl eventService;
    private final OrderService orderService;

    public EventController(EventServiceImpl eventService, OrderService orderService) {
        this.eventService = eventService;
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@RequestBody CreateEventRequest request){
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{eventId}/buy")
    public ResponseEntity<ApiResponse<BuyTicketResponse>> buyTicket(@RequestBody BuyTicketRequest request, @PathVariable UUID eventId){
        Optional<BuyTicketResponse> response = orderService.buyTicket(request, eventId);
        if (response.isPresent()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response.get()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null));
    }
}
