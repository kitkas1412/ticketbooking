package me.kitkas1412.ticketbooking.service.impl;

import me.kitkas1412.ticketbooking.dto.request.BuyTicketRequest;
import me.kitkas1412.ticketbooking.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Order;
import me.kitkas1412.ticketbooking.entity.OrderItem;
import me.kitkas1412.ticketbooking.entity.OutboxEvent;
import me.kitkas1412.ticketbooking.exception.EventNotFoundException;
import me.kitkas1412.ticketbooking.exception.NoTicketAvailableException;
import me.kitkas1412.ticketbooking.exception.OrderNotFoundException;
import me.kitkas1412.ticketbooking.mapper.TicketMapper;
import me.kitkas1412.ticketbooking.rabbitmq.BuyTicketMessage;
import me.kitkas1412.ticketbooking.rabbitmq.RabbitMQConfig;
import me.kitkas1412.ticketbooking.redis.TicketInventoryKey;
import me.kitkas1412.ticketbooking.repository.EventRepository;
import me.kitkas1412.ticketbooking.repository.OrderItemRepository;
import me.kitkas1412.ticketbooking.repository.OrderRepository;
import me.kitkas1412.ticketbooking.repository.OutboxEventRepository;
import me.kitkas1412.ticketbooking.service.OrderService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TicketMapper ticketMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OrderServiceImpl(EventRepository eventRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, OutboxEventRepository outboxEventRepository, TicketMapper ticketMapper, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.ticketMapper = ticketMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Optional<BuyTicketAcceptedResponse> buyTicket(BuyTicketRequest request, UUID eventId) {
        String key = TicketInventoryKey.availableTickets(eventId);
        String idempotencyKey = TicketInventoryKey.idempotencyKey(request.idempotencyKey());

        if (!redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofMillis(300000))){
            return Optional.empty();
        }


        if(redisTemplate.opsForValue().decrement(key) < 0){
            redisTemplate.opsForValue().increment(key);
            if (!eventRepository.existsById(eventId)){
                throw new EventNotFoundException("Không tìm thấy Event!");
            }
            throw new NoTicketAvailableException("Hết vé!");
        }

        try {
            Event event = findEventByIdOrThrow(eventId);

            Order order = orderRepository.save(Order.builder()
                    .idempotencyKey(request.idempotencyKey())
                    .event(event)
                    .build());

            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(order.getId())
                    .eventType(RabbitMQConfig.TICKET_BUY_REQUESTED_EVENT)
                    .payload(objectMapper.writeValueAsString(new BuyTicketMessage(eventId, order.getId())))
                    .build());

            return Optional.of(ticketMapper.toBuyTicketAcceptedResponse(order));
        } catch (RuntimeException e){
            redisTemplate.opsForValue().increment(key);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Object getOrderStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy Order!"));

        if (order.getStatus() == Order.OrderStatus.CONFIRMED) {
            OrderItem orderItem = orderItemRepository.findByOrder(order)
                    .orElseThrow(() -> new IllegalStateException("Order CONFIRMED nhưng không có OrderItem: " + orderId));
            return ticketMapper.toBuyTicketResponse(orderItem.getTicket(), order);
        }

        return ticketMapper.toBuyTicketAcceptedResponse(order);
    }

    private Event findEventByIdOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Không tìm thấy Event!"));
    }
}
