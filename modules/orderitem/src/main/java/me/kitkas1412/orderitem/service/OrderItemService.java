package me.kitkas1412.orderitem.service;

import me.kitkas1412.ticketbooking.entity.Order;
import me.kitkas1412.ticketbooking.entity.OrderItem;
import me.kitkas1412.ticketbooking.entity.Ticket;

import java.math.BigDecimal;

public interface OrderItemService {
    OrderItem createOrderItem(Order order, Ticket ticket, BigDecimal price);
}
