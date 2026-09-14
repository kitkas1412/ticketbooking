package me.kitkas1412.mq;

import java.util.UUID;

/**
 * Message mua vé gửi qua RabbitMQ, mang ID sự kiện và ID đơn hàng cần xử lý.
 */
public record BuyTicketMessage(UUID eventId, UUID orderId) {
}
