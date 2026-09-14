package me.kitkas1412.order.repository;

import me.kitkas1412.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Lưu và truy vấn đơn hàng theo UUID.
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {
}
