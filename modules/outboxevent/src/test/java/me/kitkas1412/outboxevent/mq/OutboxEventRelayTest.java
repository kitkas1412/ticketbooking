package me.kitkas1412.outboxevent.mq;

import me.kitkas1412.config.RabbitMQConfig;
import me.kitkas1412.mq.BuyTicketMessage;
import me.kitkas1412.outboxevent.entity.OutboxEvent;
import me.kitkas1412.outboxevent.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private OutboxEventRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxEventRelay(outboxEventRepository, rabbitTemplate, objectMapper);
    }

    @Test
    void buyRequestIsSentToTicketExchangeAndMarkedPublished() {
        BuyTicketMessage message = new BuyTicketMessage(UUID.randomUUID(), UUID.randomUUID());
        OutboxEvent event = outboxEvent(RabbitMQConfig.TICKET_BUY_REQUESTED_EVENT, objectMapper.writeValueAsString(message));

        relay.publish(event.getId());

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.TICKET_EXCHANGE, RabbitMQConfig.TICKET_BUY_ROUTING_KEY, message);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void missingOutboxRecordIsIgnored() {
        UUID id = UUID.randomUUID();
        when(outboxEventRepository.findById(id)).thenReturn(Optional.empty());

        relay.publish(id);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void alreadyPublishedRecordIsNotSentAgain() {
        OutboxEvent event = outboxEvent(RabbitMQConfig.TICKET_BUY_REQUESTED_EVENT, "{}");
        OffsetDateTime publishedAt = OffsetDateTime.now().minusMinutes(1);
        event.setPublishedAt(publishedAt);

        relay.publish(event.getId());

        verifyNoInteractions(rabbitTemplate);
        assertThat(event.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void brokerFailureLeavesRecordUnpublishedForRetry() {
        BuyTicketMessage message = new BuyTicketMessage(UUID.randomUUID(), UUID.randomUUID());
        OutboxEvent event = outboxEvent(RabbitMQConfig.TICKET_BUY_REQUESTED_EVENT, objectMapper.writeValueAsString(message));
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        relay.publish(event.getId());

        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void unknownEventTypeIsNotSentNorMarkedPublished() {
        OutboxEvent event = outboxEvent("SomethingElse", "{}");

        relay.publish(event.getId());

        verifyNoInteractions(rabbitTemplate);
        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void malformedPayloadIsNotSentNorMarkedPublished() {
        OutboxEvent event = outboxEvent(RabbitMQConfig.TICKET_BUY_REQUESTED_EVENT, "not json");

        relay.publish(event.getId());

        verifyNoInteractions(rabbitTemplate);
        assertThat(event.getPublishedAt()).isNull();
    }

    private OutboxEvent outboxEvent(String eventType, String payload) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(UUID.randomUUID())
                .eventType(eventType)
                .payload(payload)
                .build();
        event.setId(UUID.randomUUID());
        when(outboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        return event;
    }
}
