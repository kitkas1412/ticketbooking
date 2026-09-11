package me.kitkas1412.orderitem.service;

import me.kitkas1412.order.entity.Order;
import me.kitkas1412.orderitem.entity.OrderItem;
import me.kitkas1412.ticket.entity.Ticket;

import java.math.BigDecimal;

public interface OrderItemService {
    OrderItem createOrderItem(Order order, Ticket ticket, BigDecimal price);
}
