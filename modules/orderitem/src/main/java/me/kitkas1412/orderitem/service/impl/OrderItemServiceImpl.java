package me.kitkas1412.orderitem.service.impl;

import me.kitkas1412.orderitem.entity.OrderItem;
import me.kitkas1412.orderitem.repository.OrderItemRepository;
import me.kitkas1412.orderitem.service.OrderItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemServiceImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public OrderItem createOrderItem(UUID orderId, UUID ticketId, BigDecimal price) {
        return orderItemRepository.save(OrderItem.builder().orderId(orderId).ticketId(ticketId).price(price).build());
    }
}
