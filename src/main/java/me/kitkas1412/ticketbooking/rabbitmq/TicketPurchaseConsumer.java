package me.kitkas1412.ticketbooking.rabbitmq;

import me.kitkas1412.ticketbooking.entity.Event;
import me.kitkas1412.ticketbooking.entity.Order;
import me.kitkas1412.ticketbooking.entity.Ticket;
import me.kitkas1412.ticketbooking.exception.NoTicketAvailableException;
import me.kitkas1412.ticketbooking.redis.TicketInventoryKey;
import me.kitkas1412.ticketbooking.repository.OrderRepository;
import me.kitkas1412.ticketbooking.service.OrderItemService;
import me.kitkas1412.ticketbooking.service.TicketService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TicketPurchaseConsumer {

    private final OrderRepository orderRepository;
    private final TicketService ticketService;
    private final OrderItemService orderItemService;
    private final StringRedisTemplate redisTemplate;

    public TicketPurchaseConsumer(OrderRepository orderRepository, TicketService ticketService, OrderItemService orderItemService, StringRedisTemplate redisTemplate) {
        this.orderRepository = orderRepository;
        this.ticketService = ticketService;
        this.orderItemService = orderItemService;
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.TICKET_BUY_QUEUE)
    @Transactional
    public void handle(BuyTicketMessage message) {
        Order order = orderRepository.findById(message.orderId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy Order: " + message.orderId()));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            return;
        }

        Event event = order.getEvent();

        try {
            Ticket ticket = ticketService.reserveTicket(event);
            orderItemService.createOrderItem(order, ticket, ticket.getPrice());
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
        } catch (NoTicketAvailableException e) {
            order.setStatus(Order.OrderStatus.FAILED);
            orderRepository.save(order);
            redisTemplate.opsForValue().increment(TicketInventoryKey.availableTickets(message.eventId()));
        }
    }
}
