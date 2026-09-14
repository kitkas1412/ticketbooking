package me.kitkas1412.outboxevent.mq;

import me.kitkas1412.outboxevent.entity.OutboxEvent;
import me.kitkas1412.outboxevent.repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Định kỳ lấy các bản ghi outbox chưa gửi và chuyển từng bản ghi cho OutboxEventRelay.
 * Relay xử lý transaction riêng; bản ghi chưa được đánh dấu sẽ được đọc lại ở lượt sau.
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
