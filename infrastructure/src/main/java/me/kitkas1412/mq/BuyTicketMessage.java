package me.kitkas1412.mq;

import java.util.UUID;

public record BuyTicketMessage(UUID eventId, UUID orderId) {
}
