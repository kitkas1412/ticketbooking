package me.kitkas1412.order.service;

import me.kitkas1412.ticket.dto.request.BuyTicketRequest;
import me.kitkas1412.ticket.dto.response.BuyTicketAcceptedResponse;

import java.util.Optional;
import java.util.UUID;

public interface OrderService {
    Optional<BuyTicketAcceptedResponse> buyTicket(BuyTicketRequest request, UUID eventId) throws Exception;

    Object getOrderStatus(UUID orderId);
}
