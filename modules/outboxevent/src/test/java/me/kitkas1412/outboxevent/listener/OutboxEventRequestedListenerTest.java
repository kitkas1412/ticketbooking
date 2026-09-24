package me.kitkas1412.outboxevent.listener;

import me.kitkas1412.common.event.OutboxEventRequestedEvent;
import me.kitkas1412.outboxevent.entity.OutboxEvent;
import me.kitkas1412.outboxevent.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventRequestedListenerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventRequestedListener listener;

    @Test
    void persistsUnpublishedOutboxRecord() {
        UUID orderId = UUID.randomUUID();
        String payload = "{\"eventId\":\"e\",\"orderId\":\"o\"}";

        listener.handle(new OutboxEventRequestedEvent(this, "ORDER", orderId, "TicketBuyRequested", payload));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("ORDER");
        assertThat(saved.getAggregateId()).isEqualTo(orderId);
        assertThat(saved.getEventType()).isEqualTo("TicketBuyRequested");
        assertThat(saved.getPayload()).isEqualTo(payload);
        assertThat(saved.getPublishedAt()).isNull();
    }
}
