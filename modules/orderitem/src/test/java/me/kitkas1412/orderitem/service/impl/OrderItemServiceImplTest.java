package me.kitkas1412.orderitem.service.impl;

import me.kitkas1412.orderitem.entity.OrderItem;
import me.kitkas1412.orderitem.repository.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    @Test
    void createOrderItemPersistsOrderTicketAndPurchasePrice() {
        UUID orderId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("350000.00");
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItem result = orderItemService.createOrderItem(orderId, ticketId, price);

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getTicketId()).isEqualTo(ticketId);
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo(price);
        assertThat(result).isSameAs(captor.getValue());
    }
}
