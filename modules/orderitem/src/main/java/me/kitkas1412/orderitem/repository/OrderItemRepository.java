package me.kitkas1412.orderitem.repository;


import me.kitkas1412.orderitem.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Lưu và truy vấn chi tiết đơn hàng theo UUID.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
