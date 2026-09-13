package me.kitkas1412.orderitem.service;

import me.kitkas1412.orderitem.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public interface OrderItemService {
    OrderItem createOrderItem(UUID order, UUID ticket, BigDecimal price);
}
