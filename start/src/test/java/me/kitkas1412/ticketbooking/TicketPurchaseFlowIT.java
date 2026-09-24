package me.kitkas1412.ticketbooking;

import me.kitkas1412.cache.TicketInventoryKey;
import me.kitkas1412.common.dto.request.BuyTicketRequest;
import me.kitkas1412.common.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.common.exception.NoTicketAvailableException;
import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.order.entity.Order;
import me.kitkas1412.order.service.OrderService;
import me.kitkas1412.orderitem.repository.OrderItemRepository;
import me.kitkas1412.outboxevent.repository.OutboxEventRepository;
import me.kitkas1412.ticket.entity.Ticket;
import me.kitkas1412.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Luồng mua vé qua Redis → PostgreSQL (order + outbox) → RabbitMQ → consumer cấp vé.
 */
class TicketPurchaseFlowIT extends AbstractIntegrationTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(20);

    @Autowired
    private OrderService orderService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void purchaseIsConfirmedThroughOutboxAndRabbitMq() throws Exception {
        UUID eventId = createEvent(3);

        BuyTicketAcceptedResponse accepted = orderService
                .buyTicket(new BuyTicketRequest(UUID.randomUUID().toString()), eventId)
                .orElseThrow();

        assertThat(accepted.status()).isEqualTo("PENDING");
        assertThat(accepted.eventId()).isEqualTo(eventId);
        assertThat(availableInRedis(eventId)).isEqualTo(2);
        assertThat(outboxEventRepository.findAll())
                .anySatisfy(outbox -> assertThat(outbox.getAggregateId()).isEqualTo(accepted.orderId()));

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            Order order = orderRepository.findById(accepted.orderId()).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        });
        assertThat(ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.SOLD)).isEqualTo(1);
        assertThat(ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.AVAILABLE)).isEqualTo(2);
        assertThat(orderItemRepository.findAll())
                .filteredOn(item -> item.getOrderId().equals(accepted.orderId()))
                .hasSize(1);
        assertThat(outboxEventRepository.findAll())
                .filteredOn(outbox -> outbox.getAggregateId().equals(accepted.orderId()))
                .allSatisfy(outbox -> assertThat(outbox.getPublishedAt()).isNotNull());
    }

    @Test
    void sameIdempotencyKeyCreatesOnlyOneOrder() throws Exception {
        UUID eventId = createEvent(3);
        BuyTicketRequest request = new BuyTicketRequest(UUID.randomUUID().toString());

        Optional<BuyTicketAcceptedResponse> first = orderService.buyTicket(request, eventId);
        Optional<BuyTicketAcceptedResponse> retry = orderService.buyTicket(request, eventId);

        assertThat(first).isPresent();
        assertThat(retry).isEmpty();
        assertThat(availableInRedis(eventId)).isEqualTo(2);
        assertThat(ordersOf(eventId)).hasSize(1);
    }

    @Test
    void neverSellsMoreTicketsThanAvailableUnderConcurrentLoad() throws Exception {
        int totalTickets = 5;
        int buyers = 40;
        UUID eventId = createEvent(totalTickets);

        AtomicInteger soldOut = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Optional<BuyTicketAcceptedResponse>>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(buyers)) {
            for (int i = 0; i < buyers; i++) {
                Callable<Optional<BuyTicketAcceptedResponse>> buy = () -> {
                    start.await();
                    try {
                        return orderService.buyTicket(new BuyTicketRequest(UUID.randomUUID().toString()), eventId);
                    } catch (NoTicketAvailableException e) {
                        soldOut.incrementAndGet();
                        return Optional.empty();
                    }
                };
                results.add(pool.submit(buy));
            }
            start.countDown();
        }

        long accepted = 0;
        for (Future<Optional<BuyTicketAcceptedResponse>> result : results) {
            if (result.get().isPresent()) {
                accepted++;
            }
        }
        assertThat(accepted).isEqualTo(totalTickets);
        assertThat(soldOut).hasValue(buyers - totalTickets);
        assertThat(availableInRedis(eventId)).isZero();

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> assertThat(ordersOf(eventId))
                .hasSize(totalTickets)
                .allSatisfy(order -> assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED)));
        assertThat(ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.SOLD)).isEqualTo(totalTickets);
        assertThat(ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.AVAILABLE)).isZero();
    }

    @Test
    void rejectsUnknownEvent() {
        UUID unknownEventId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.buyTicket(new BuyTicketRequest(UUID.randomUUID().toString()), unknownEventId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(ordersOf(unknownEventId)).isEmpty();
    }

    @Test
    void restoresInventoryWhenOrderTransactionRollsBack() throws Exception {
        UUID eventId = createEvent(3);
        String idempotencyKey = UUID.randomUUID().toString();
        orderService.buyTicket(new BuyTicketRequest(idempotencyKey), eventId).orElseThrow();
        assertThat(availableInRedis(eventId)).isEqualTo(2);

        // Mất khoá Redis (vd: hết TTL) nhưng đơn đã có trong DB: lần gửi lại vượt qua kiểm tra
        // Redis, trừ tồn kho, rồi vi phạm unique idempotency_key lúc commit và bị rollback.
        redisTemplate.delete(TicketInventoryKey.idempotencyKey(idempotencyKey));

        assertThatThrownBy(() -> orderService.buyTicket(new BuyTicketRequest(idempotencyKey), eventId))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(availableInRedis(eventId)).isEqualTo(2);
        assertThat(redisTemplate.hasKey(TicketInventoryKey.idempotencyKey(idempotencyKey))).isFalse();
        assertThat(ordersOf(eventId)).hasSize(1);
    }
}
