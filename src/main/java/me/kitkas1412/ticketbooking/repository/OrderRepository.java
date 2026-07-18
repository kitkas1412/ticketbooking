package me.kitkas1412.ticketbooking.repository;

import me.kitkas1412.ticketbooking.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
