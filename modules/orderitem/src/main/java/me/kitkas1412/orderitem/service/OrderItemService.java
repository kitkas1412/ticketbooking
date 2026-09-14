package me.kitkas1412.orderitem.service;

import me.kitkas1412.orderitem.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interface tạo OrderItem từ ID đơn hàng, ID vé và giá mua.
 */
public interface OrderItemService {
    // Lưu giá mua cùng liên kết UUID để không phụ thuộc việc thay đổi giá vé về sau.
    OrderItem createOrderItem(UUID order, UUID ticket, BigDecimal price);
}
