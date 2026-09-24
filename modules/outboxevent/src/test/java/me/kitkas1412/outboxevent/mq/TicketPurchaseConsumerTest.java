package me.kitkas1412.outboxevent.mq;

import me.kitkas1412.cache.TicketInventoryKey;
import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.mq.BuyTicketMessage;
import me.kitkas1412.order.entity.Order;
import me.kitkas1412.order.repository.OrderRepository;
import me.kitkas1412.orderitem.service.OrderItemService;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketPurchaseConsumerTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TicketService ticketService;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private TicketPurchaseConsumer consumer;

    private final UUID eventId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        consumer = new TicketPurchaseConsumer(orderRepository, ticketService, orderItemService, redisTemplate);
    }

    @Test
    void pendingOrderGetsTicketAndIsConfirmed() {
        Order order = order(Order.OrderStatus.PENDING);
        Ticket ticket = Ticket.builder().eventId(eventId).seatCode(7).price(new BigDecimal("350000")).build();
        ticket.setId(UUID.randomUUID());
        when(ticketService.reserveTicket(eventId)).thenReturn(ticket);

        consumer.handle(new BuyTicketMessage(eventId, orderId));

        verify(orderItemService).createOrderItem(orderId, ticket.getId(), ticket.getPrice());
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
        verifyNoInteractions(valueOperations);
    }

    @Test
    void noTicketInDatabaseFailsOrderAndReturnsInventoryToRedis() {
        Order order = order(Order.OrderStatus.PENDING);
        when(ticketService.reserveTicket(eventId)).thenThrow(new ResourceNotFoundException("Không tìm thấy Ticket"));

        consumer.handle(new BuyTicketMessage(eventId, orderId));

        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.FAILED);
        verify(orderRepository).save(order);
        verify(valueOperations).increment(TicketInventoryKey.availableTickets(eventId));
        verifyNoInteractions(orderItemService);
    }

    @ParameterizedTest
    @EnumSource(value = Order.OrderStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void redeliveredMessageForProcessedOrderIsIgnored(Order.OrderStatus status) {
        Order order = order(status);

        consumer.handle(new BuyTicketMessage(eventId, orderId));

        assertThat(order.getStatus()).isEqualTo(status);
        verifyNoInteractions(ticketService, orderItemService, valueOperations);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void unknownOrderIsRejected() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consumer.handle(new BuyTicketMessage(eventId, orderId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(orderId.toString());
        verifyNoInteractions(ticketService, orderItemService);
    }

    @Test
    void unexpectedFailureIsPropagatedSoTransactionRollsBack() {
        order(Order.OrderStatus.PENDING);
        when(ticketService.reserveTicket(eventId)).thenThrow(new IllegalStateException("optimistic lock"));

        assertThatThrownBy(() -> consumer.handle(new BuyTicketMessage(eventId, orderId)))
                .isInstanceOf(IllegalStateException.class);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(valueOperations);
    }

    private Order order(Order.OrderStatus status) {
        Order order = Order.builder().event_id(eventId).idempotencyKey("key-1").status(status).build();
        order.setId(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        return order;
    }
}
