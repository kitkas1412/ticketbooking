package me.kitkas1412.ticketbooking.service.impl;

import me.kitkas1412.ticketbooking.dto.request.BuyTicketRequest;
import me.kitkas1412.ticketbooking.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Order;
import me.kitkas1412.ticketbooking.exception.EventNotFoundException;
import me.kitkas1412.ticketbooking.exception.NoTicketAvailableException;
import me.kitkas1412.ticketbooking.mapper.TicketMapper;
import me.kitkas1412.ticketbooking.rabbitmq.BuyTicketMessage;
import me.kitkas1412.ticketbooking.rabbitmq.RabbitMQConfig;
import me.kitkas1412.ticketbooking.redis.TicketInventoryKey;
import me.kitkas1412.ticketbooking.repository.EventRepository;
import me.kitkas1412.ticketbooking.repository.OrderRepository;
import me.kitkas1412.ticketbooking.service.OrderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketMapper ticketMapper;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OrderServiceImpl(EventRepository eventRepository, OrderRepository orderRepository, TicketMapper ticketMapper, StringRedisTemplate redisTemplate, RabbitTemplate rabbitTemplate) {
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.ticketMapper = ticketMapper;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
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

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TICKET_EXCHANGE,
                    RabbitMQConfig.TICKET_BUY_ROUTING_KEY,
                    new BuyTicketMessage(eventId, order.getId()));

            return Optional.of(ticketMapper.toBuyTicketAcceptedResponse(order));
        } catch (RuntimeException e){
            redisTemplate.opsForValue().increment(key);
            throw e;
        }
    }

    private Event findEventByIdOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Không tìm thấy Event!"));
    }
}
