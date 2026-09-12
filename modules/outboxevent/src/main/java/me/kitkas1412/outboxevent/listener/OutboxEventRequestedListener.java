package me.kitkas1412.outboxevent.listener;

import me.kitkas1412.common.event.OutboxEventRequestedEvent;
import me.kitkas1412.outboxevent.entity.OutboxEvent;
import me.kitkas1412.outboxevent.repository.OutboxEventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventRequestedListener {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventRequestedListener(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @EventListener
    public void handle(OutboxEventRequestedEvent event) {
        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .build());
    }
}
