package me.kitkas1412.ticketbooking.service;

import me.kitkas1412.ticketbooking.dto.request.BuyTicketRequest;
import me.kitkas1412.ticketbooking.dto.response.BuyTicketAcceptedResponse;

import java.util.Optional;
import java.util.UUID;

public interface OrderService {
    Optional<BuyTicketAcceptedResponse> buyTicket(BuyTicketRequest request, UUID eventId);
}
