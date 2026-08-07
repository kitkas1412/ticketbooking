package me.kitkas1412.ticketbooking.rabbitmq;

import me.kitkas1412.ticketbooking.entity.OutboxEvent;
import me.kitkas1412.ticketbooking.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : pendingEvents) {
            publishEvent(event.getId());
        }
    }

    @Transactional
    public void publishEvent(UUID outboxEventId) {
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
