package me.kitkas1412.order.service.impl;

import me.kitkas1412.cache.TicketInventoryKey;
import me.kitkas1412.common.dto.request.BuyTicketRequest;
import me.kitkas1412.common.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.common.event.EventCheckRequiredEvent;
import me.kitkas1412.common.event.OutboxEventRequestedEvent;
import me.kitkas1412.common.exception.NoTicketAvailableException;
import me.kitkas1412.common.exception.ResourceNotFoundException;
import me.kitkas1412.config.RabbitMQConfig;
import me.kitkas1412.mq.BuyTicketMessage;
import me.kitkas1412.order.entity.Order;
import me.kitkas1412.order.mapper.OrderMapper;
import me.kitkas1412.order.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final String CLIENT_KEY = "client-key-1";

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private OrderServiceImpl orderService;

    private final UUID eventId = UUID.randomUUID();
    private final String inventoryKey = TicketInventoryKey.availableTickets(eventId);
    private final String idempotencyKey = TicketInventoryKey.idempotencyKey(CLIENT_KEY);

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        orderService = new OrderServiceImpl(orderRepository, redisTemplate, objectMapper, orderMapper, eventPublisher);
        // buyTicket đăng ký callback bù tồn kho, việc này yêu cầu transaction synchronization đang bật.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void duplicateIdempotencyKeyReturnsEmptyWithoutTouchingInventory() throws Exception {
        when(valueOperations.setIfAbsent(eq(idempotencyKey), eq("1"), any(Duration.class))).thenReturn(false);

        Optional<BuyTicketAcceptedResponse> response = orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId);

        assertThat(response).isEmpty();
        verify(valueOperations, never()).decrement(anyString());
        verifyNoInteractions(orderRepository, eventPublisher);
    }

    @Test
    void idempotencyKeyIsReservedForFiveMinutes() throws Exception {
        when(valueOperations.setIfAbsent(eq(idempotencyKey), eq("1"), any(Duration.class))).thenReturn(false);

        orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId);

        verify(valueOperations).setIfAbsent(idempotencyKey, "1", Duration.ofMinutes(5));
    }

    @Test
    void soldOutRestoresCounterAndThrowsNoTicketAvailable() {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(-1L);
        answerEventCheck(event -> event.setEventExists(true));

        assertThatThrownBy(() -> orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId))
                .isInstanceOf(NoTicketAvailableException.class)
                .hasMessage("Hết vé!");

        verify(valueOperations).increment(inventoryKey);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void missingEventRestoresCounterAndThrowsNotFound() {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(-1L);
        answerEventCheck(event -> event.setEventExists(false));

        assertThatThrownBy(() -> orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy Event!");

        verify(valueOperations).increment(inventoryKey);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void eventCheckFailureIsRethrown() {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(-1L);
        ResourceNotFoundException failure = new ResourceNotFoundException("Không tìm thấy Event!");
        answerEventCheck(event -> event.setException(failure));

        assertThatThrownBy(() -> orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId))
                .isSameAs(failure);
    }

    @Test
    void eventCheckIsPublishedForTheRequestedEvent() {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(-1L);

        assertThatThrownBy(() -> orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId))
                .isInstanceOf(ResourceNotFoundException.class);

        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOfSatisfying(EventCheckRequiredEvent.class,
                event -> assertThat(event.getEventId()).isEqualTo(eventId));
    }

    @Test
    void lastTicketCanStillBeReserved() throws Exception {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(0L);
        givenOrderIsSaved();

        assertThat(orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId)).isPresent();

        verify(valueOperations, never()).increment(inventoryKey);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void successfulReservationCreatesPendingOrderAndOutboxMessage() throws Exception {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(9L);
        UUID orderId = givenOrderIsSaved();
        BuyTicketAcceptedResponse accepted = new BuyTicketAcceptedResponse(orderId, eventId, "PENDING");
        when(orderMapper.toBuyTicketAcceptedResponse(any(Order.class))).thenReturn(accepted);

        Optional<BuyTicketAcceptedResponse> response = orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId);

        assertThat(response).contains(accepted);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getEvent_id()).isEqualTo(eventId);
        assertThat(orderCaptor.getValue().getIdempotencyKey()).isEqualTo(CLIENT_KEY);
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        ArgumentCaptor<ApplicationEvent> published = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher).publishEvent(published.capture());
        assertThat(published.getValue()).isInstanceOfSatisfying(OutboxEventRequestedEvent.class, outbox -> {
            assertThat(outbox.getAggregateType()).isEqualTo("ORDER");
            assertThat(outbox.getAggregateId()).isEqualTo(orderId);
            assertThat(outbox.getEventType()).isEqualTo(RabbitMQConfig.TICKET_BUY_REQUESTED_EVENT);
            assertThat(objectMapper.readValue(outbox.getPayload(), BuyTicketMessage.class))
                    .isEqualTo(new BuyTicketMessage(eventId, orderId));
        });
    }

    @Test
    void rollbackAfterReservationRestoresInventoryAndReleasesIdempotencyKey() throws Exception {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(4L);
        givenOrderIsSaved();

        orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(valueOperations).increment(inventoryKey);
        verify(redisTemplate).delete(idempotencyKey);
    }

    @Test
    void unknownCompletionStatusIsCompensatedLikeRollback() throws Exception {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(4L);
        givenOrderIsSaved();

        orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId);
        completeTransaction(TransactionSynchronization.STATUS_UNKNOWN);

        verify(valueOperations).increment(inventoryKey);
        verify(redisTemplate).delete(idempotencyKey);
    }

    @Test
    void commitKeepsReservation() throws Exception {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(4L);
        givenOrderIsSaved();

        orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId);
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(valueOperations, never()).increment(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void failureWhileSavingOrderIsCompensatedOnRollback() {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(4L);
        when(orderRepository.save(any(Order.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId))
                .isInstanceOf(IllegalStateException.class);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(valueOperations).increment(inventoryKey);
        verify(redisTemplate).delete(idempotencyKey);
    }

    @Test
    void rejectedRequestsRegisterNoCompensation() {
        givenIdempotencyKeyIsNew();
        when(valueOperations.decrement(inventoryKey)).thenReturn(-1L);
        answerEventCheck(event -> event.setEventExists(true));

        assertThatThrownBy(() -> orderService.buyTicket(new BuyTicketRequest(CLIENT_KEY), eventId))
                .isInstanceOf(NoTicketAvailableException.class);

        // Counter đã được trả ngay, nếu có thêm callback thì rollback sẽ cộng tồn kho hai lần.
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    private void givenIdempotencyKeyIsNew() {
        when(valueOperations.setIfAbsent(eq(idempotencyKey), eq("1"), any(Duration.class))).thenReturn(true);
    }

    private UUID givenOrderIsSaved() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            return order;
        });
        lenient().when(orderMapper.toBuyTicketAcceptedResponse(any(Order.class)))
                .thenAnswer(invocation -> new BuyTicketAcceptedResponse(orderId, eventId, "PENDING"));
        return orderId;
    }

    private void answerEventCheck(java.util.function.Consumer<EventCheckRequiredEvent> listener) {
        doAnswer(invocation -> {
            listener.accept(invocation.getArgument(0));
            return null;
        }).when(eventPublisher).publishEvent(any(EventCheckRequiredEvent.class));
    }

    private static void completeTransaction(int status) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
    }
}
