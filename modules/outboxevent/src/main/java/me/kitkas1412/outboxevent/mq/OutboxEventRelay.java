package me.kitkas1412.outboxevent.mq;

import me.kitkas1412.config.RabbitMQConfig;
import me.kitkas1412.mq.BuyTicketMessage;
import me.kitkas1412.outboxevent.entity.OutboxEvent;
import me.kitkas1412.outboxevent.repository.OutboxEventRepository;
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
 * Gửi từng bản ghi outbox tới RabbitMQ và ghi thời điểm gửi.
 *
 * <p>Tách thành bean riêng để lời gọi từ OutboxEventPublisher đi qua proxy của Spring,
 * nhờ đó @Transactional có hiệu lực. Gọi trực tiếp trong cùng bean sẽ bỏ qua proxy.
 *
 * <p>Mỗi bản ghi chạy trong transaction riêng để cập nhật trạng thái gửi độc lập.
 * Gửi message qua RabbitMQ và commit DB không thuộc cùng một atomic transaction;
 * message có thể được gửi lại nếu commit thất bại sau khi đã gửi.
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
            // Đánh dấu sau khi lệnh gửi trả về; đoạn này chưa chờ publisher confirm từ broker.
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
