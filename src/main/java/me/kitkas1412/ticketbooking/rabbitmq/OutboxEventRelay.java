package me.kitkas1412.ticketbooking.rabbitmq;

import me.kitkas1412.ticketbooking.entity.OutboxEvent;
import me.kitkas1412.ticketbooking.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Publishes a single outbox row to RabbitMQ and marks it published.
 *
 * <p>This lives in its own bean on purpose. It used to be a method on
 * {@link OutboxEventPublisher} called directly from the scheduled poller, which
 * meant the call never went through the Spring proxy and {@code @Transactional}
 * did nothing: the {@code publishedAt} write was silently discarded, the poller
 * kept re-reading the same oldest rows forever, and no event past the first page
 * was ever delivered. Going through a separate bean makes the proxy — and so the
 * transaction — real.
 *
 * <p>Each event gets its own transaction so one undeliverable row cannot roll
 * back the rest of the batch.
 */
@Component
public class OutboxEventRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventRelay(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(UUID outboxEventId) {
        OutboxEvent event = outboxEventRepository.findById(outboxEventId).orElse(null);
        if (event == null || event.getPublishedAt() != null) {
            return;
        }

        try {
            route(event);
            event.setPublishedAt(OffsetDateTime.now());
        } catch (Exception e) {
            log.error("Không publish được OutboxEvent {} (eventType={})", event.getId(), event.getEventType(), e);
        }
    }

    private void route(OutboxEvent event) {
        if (RabbitMQConfig.TICKET_BUY_REQUESTED_EVENT.equals(event.getEventType())) {
            BuyTicketMessage message = objectMapper.readValue(event.getPayload(), BuyTicketMessage.class);
            rabbitTemplate.convertAndSend(RabbitMQConfig.TICKET_EXCHANGE, RabbitMQConfig.TICKET_BUY_ROUTING_KEY, message);
            return;
        }

        throw new IllegalStateException("Không nhận diện được OutboxEvent eventType: " + event.getEventType());
    }
}
