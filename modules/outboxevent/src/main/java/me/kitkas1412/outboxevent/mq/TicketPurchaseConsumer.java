package me.kitkas1412.outboxevent.mq;

import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.config.RabbitMQConfig;
import me.kitkas1412.event.entity.Event;
import me.kitkas1412.mq.BuyTicketMessage;
import me.kitkas1412.order.entity.Order;
import me.kitkas1412.order.repository.OrderRepository;
import me.kitkas1412.orderitem.service.OrderItemService;
import me.kitkas1412.ticket.cache.TicketInventoryKey;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.ticket.service.TicketService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhận yêu cầu mua vé từ RabbitMQ để cấp vé và cập nhật kết quả đơn hàng.
 * Các lời gọi vẫn truyền entity theo mô hình cũ; cần cập nhật để khớp service nhận UUID.
 */
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

        // Bỏ qua message gửi lại nếu đơn đã xử lý xong; bước này không ngăn hai consumer chạy đồng thời.
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            return;
        }

        Event event = order.getEvent();

        try {
            Ticket ticket = ticketService.reserveTicket(event);
            orderItemService.createOrderItem(order, ticket, ticket.getPrice());
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
        // Không lấy được vé thì chuyển đơn sang FAILED và tăng lại tồn kho Redis.
        } catch (ResourceNotFoundException e) {
            order.setStatus(Order.OrderStatus.FAILED);
            orderRepository.save(order);
            redisTemplate.opsForValue().increment(TicketInventoryKey.availableTickets(message.eventId()));
        }
    }
}
