package me.kitkas1412.order.service.impl;

import me.kitkas1412.common.event.EventCheckRequiredEvent;
import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.config.RabbitMQConfig;
import me.kitkas1412.event.entity.Event;
import me.kitkas1412.event.repository.EventRepository;
import me.kitkas1412.order.entity.Order;
import me.kitkas1412.order.repository.OrderRepository;
import me.kitkas1412.order.service.OrderService;
import me.kitkas1412.orderitem.entity.OrderItem;
import me.kitkas1412.orderitem.repository.OrderItemRepository;
import me.kitkas1412.outboxevent.entity.OutboxEvent;
import me.kitkas1412.outboxevent.mq.BuyTicketMessage;
import me.kitkas1412.outboxevent.repository.OutboxEventRepository;
import me.kitkas1412.ticket.cache.TicketInventoryKey;
import me.kitkas1412.ticket.dto.request.BuyTicketRequest;
import me.kitkas1412.ticket.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.common.exception.NoTicketAvailableException;
import me.kitkas1412.ticket.mapper.TicketMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TicketMapper ticketMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;  // ✅ NEW

    public OrderServiceImpl(EventRepository eventRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, OutboxEventRepository outboxEventRepository, TicketMapper ticketMapper, StringRedisTemplate redisTemplate, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.ticketMapper = ticketMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Optional<BuyTicketAcceptedResponse> buyTicket(BuyTicketRequest request, UUID eventId) throws Exception {
        String key = TicketInventoryKey.availableTickets(eventId);
        String idempotencyKey = TicketInventoryKey.idempotencyKey(request.idempotencyKey());

        if (!redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofMillis(300000))){
            return Optional.empty();
        }


        if(redisTemplate.opsForValue().decrement(key) < 0){
            redisTemplate.opsForValue().increment(key);

            EventCheckRequiredEvent event = new EventCheckRequiredEvent(this, eventId);
            eventPublisher.publishEvent(event);
            if (event.getException() != null){
                throw event.getException();
            }

            if (!event.isEventExists()){
                throw new ResourceNotFoundException("Không tìm thấy Event!");
            }
            throw new NoTicketAvailableException("Hết vé!");
        }

        // Inventory is now claimed in Redis but nothing is durable yet. Hand the
        // compensation to the transaction manager instead of a try/catch: this
        // method is @Transactional, so the flush and commit happen *after* it
        // returns and a failure there would never reach a catch block here --
        // leaking the claimed ticket until the reconciler happens to run.
        registerInventoryCompensation(key, idempotencyKey);

        Order order = orderRepository.save(Order.builder()
                .idempotencyKey(request.idempotencyKey())
                .event_id(eventId)
                .build());

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(order.getId())
                .eventType(RabbitMQConfig.TICKET_BUY_REQUESTED_EVENT)
                .payload(objectMapper.writeValueAsString(new BuyTicketMessage(eventId, order.getId())))
                .build());

        return Optional.of(ticketMapper.toBuyTicketAcceptedResponse(order));
    }

    /**
     * Gives back the Redis ticket claim if the surrounding transaction does not
     * commit, whether it failed inside the method body or during commit itself.
     *
     * <p>The idempotency key is released alongside it: the key is claimed before
     * any durable write, so leaving it behind after a rollback would make the
     * client's retry look like a duplicate and silently drop the purchase.
     */
    private void registerInventoryCompensation(String inventoryKey, String idempotencyKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    return;
                }
                redisTemplate.opsForValue().increment(inventoryKey);
                redisTemplate.delete(idempotencyKey);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Object getOrderStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Order!"));

        if (order.getStatus() == Order.OrderStatus.CONFIRMED) {
            OrderItem orderItem = orderItemRepository.findByOrder(order)
                    .orElseThrow(() -> new IllegalStateException("Order CONFIRMED nhưng không có OrderItem: " + orderId));
            return ticketMapper.toBuyTicketResponse(orderItem.getTicket(), order);
        }

        return ticketMapper.toBuyTicketAcceptedResponse(order);
    }
}
