package me.kitkas1412.ticketbooking.dto.response;

import me.kitkas1412.ticketbooking.entity.Order;

import java.util.UUID;

public record BuyTicketAcceptedResponse(UUID orderId, UUID eventId, Order.OrderStatus status) {
}
