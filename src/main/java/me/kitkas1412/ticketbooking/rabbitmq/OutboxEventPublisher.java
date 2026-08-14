package me.kitkas1412.ticketbooking.rabbitmq;

import me.kitkas1412.ticketbooking.entity.OutboxEvent;
import me.kitkas1412.ticketbooking.repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls the transactional outbox and hands each unpublished row to
 * {@link OutboxEventRelay}, which does the actual publish in its own
 * transaction. A failed publish leaves the row unpublished, so it is simply
 * retried on the next poll.
 */
@Component
public class OutboxEventPublisher {

    private static final int BATCH_SIZE = 200;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventRelay relay;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, OutboxEventRelay relay) {
        this.outboxEventRepository = outboxEventRepository;
        this.relay = relay;
    }

    @Scheduled(fixedDelay = 500)
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : pendingEvents) {
            relay.publish(event.getId());
        }
    }
}
