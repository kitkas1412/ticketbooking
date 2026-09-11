package me.kitkas1412.orderitem.service.impl;

import me.kitkas1412.order.entity.Order;
import me.kitkas1412.orderitem.entity.OrderItem;
import me.kitkas1412.orderitem.repository.OrderItemRepository;
import me.kitkas1412.orderitem.service.OrderItemService;
import me.kitkas1412.ticket.entity.Ticket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemServiceImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public OrderItem createOrderItem(Order order, Ticket ticket, BigDecimal price) {
        return orderItemRepository.save(OrderItem.builder()
                .order(order)
                .ticket(ticket)
                .price(price)
                .build());
    }
}
