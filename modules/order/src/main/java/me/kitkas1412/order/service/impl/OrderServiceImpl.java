package me.kitkas1412.order.service.impl;

import me.kitkas1412.cache.TicketInventoryKey;
import me.kitkas1412.common.dto.request.BuyTicketRequest;
import me.kitkas1412.common.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.common.event.EventCheckRequiredEvent;
import me.kitkas1412.common.event.OutboxEventRequestedEvent;
import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.config.RabbitMQConfig;
import me.kitkas1412.mq.BuyTicketMessage;
import me.kitkas1412.order.entity.Order;
import me.kitkas1412.order.mapper.OrderMapper;
import me.kitkas1412.order.repository.OrderRepository;
import me.kitkas1412.order.service.OrderService;
import me.kitkas1412.common.exception.NoTicketAvailableException;
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

/**
 * Trừ tồn kho Redis để giữ chỗ, sau đó tạo đơn PENDING và ghi outbox trong cùng transaction.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper, OrderMapper orderMapper, ApplicationEventPublisher eventPublisher) {
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
        this.orderRepository = orderRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Optional<BuyTicketAcceptedResponse> buyTicket(BuyTicketRequest request, UUID eventId) throws Exception {
        String key = TicketInventoryKey.availableTickets(eventId);
        String idempotencyKey = TicketInventoryKey.idempotencyKey(request.idempotencyKey());

        // Tạo idempotency key với TTL 5 phút; key đã có thì trả Optional.empty().
        if (!redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofMillis(300000))){
            return Optional.empty();
        }


        // DECR là thao tác atomic trên Redis; bộ đếm âm nghĩa là không còn vé để giữ.
        // Tăng lại bộ đếm rồi kiểm tra lỗi do sự kiện không tồn tại hay do hết vé.
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

        // Redis đã trừ tồn kho nhưng dữ liệu đơn chưa được commit.
        // Đăng ký callback hoàn tồn kho khi transaction kết thúc để xử lý cả lỗi flush/commit
        // xảy ra sau khi method trả về; try/catch ở đây không bắt được các lỗi đó.
        registerInventoryCompensation(key, idempotencyKey);

        Order order = orderRepository.save(Order.builder()
                .idempotencyKey(request.idempotencyKey())
                .event_id(eventId)
                .build());

        // Lưu outbox cùng transaction với đơn; outbox publisher gửi message qua RabbitMQ sau đó.
        eventPublisher.publishEvent(new OutboxEventRequestedEvent(
                this,
                "ORDER",
                order.getId(),
                RabbitMQConfig.TICKET_BUY_REQUESTED_EVENT,
                objectMapper.writeValueAsString(new BuyTicketMessage(eventId, order.getId()))));

        return Optional.of(orderMapper.toBuyTicketAcceptedResponse(order));
    }

    /**
     * Hoàn tồn kho nếu transaction không commit thành công, kể cả lỗi ở bước commit.
     * Xóa cả idempotency key để client có thể retry khi đơn chưa được commit vào DB.
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

//    @Override
//    @Transactional(readOnly = true)
//    public Object getOrderStatus(UUID orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Order!"));
//
//        if (order.getStatus() == Order.OrderStatus.CONFIRMED) {
//            OrderItem orderItem = orderItemRepository.findByOrder(order)
//                    .orElseThrow(() -> new IllegalStateException("Order CONFIRMED nhưng không có OrderItem: " + orderId));
//            return ticketMapper.toBuyTicketResponse(orderItem.getTicket(), order);
//        }
//
//        return ticketMapper.toBuyTicketAcceptedResponse(order);
//    }
}
