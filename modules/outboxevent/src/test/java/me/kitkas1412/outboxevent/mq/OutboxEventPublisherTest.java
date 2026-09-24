package me.kitkas1412.outboxevent.mq;

import me.kitkas1412.outboxevent.entity.OutboxEvent;
import me.kitkas1412.outboxevent.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private OutboxEventRelay relay;

    @InjectMocks
    private OutboxEventPublisher publisher;

    @Test
    void relaysPendingEventsOldestFirstInBatchesOf200() {
        OutboxEvent first = outboxEvent();
        OutboxEvent second = outboxEvent();
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 200)))
                .thenReturn(List.of(first, second));

        publisher.publishPendingEvents();

        InOrder order = inOrder(relay);
        order.verify(relay).publish(first.getId());
        order.verify(relay).publish(second.getId());
    }

    @Test
    void doesNothingWhenNoEventIsPending() {
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(any())).thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(relay, never()).publish(any());
    }

    private static OutboxEvent outboxEvent() {
        OutboxEvent event = OutboxEvent.builder().build();
        event.setId(UUID.randomUUID());
        return event;
    }
}
