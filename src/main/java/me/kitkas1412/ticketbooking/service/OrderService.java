package me.kitkas1412.ticketbooking.service;

import me.kitkas1412.ticketbooking.dto.request.BuyTicketRequest;
import me.kitkas1412.ticketbooking.dto.response.BuyTicketResponse;

import java.util.UUID;

public interface OrderService {
    BuyTicketResponse buyTicket(BuyTicketRequest request, UUID eventId);
}
