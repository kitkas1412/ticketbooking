package me.kitkas1412.outboxevent.mq;

import java.util.UUID;

public record BuyTicketMessage(UUID eventId, UUID orderId) {
}
