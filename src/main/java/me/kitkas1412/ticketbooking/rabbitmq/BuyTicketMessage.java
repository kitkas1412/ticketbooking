package me.kitkas1412.ticketbooking.rabbitmq;

import java.util.UUID;

public record BuyTicketMessage(UUID eventId, UUID orderId) {
}
