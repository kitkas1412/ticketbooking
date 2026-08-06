package me.kitkas1412.ticketbooking.controller;

import me.kitkas1412.ticketbooking.dto.response.ApiResponse;
import me.kitkas1412.ticketbooking.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<?>> getOrderStatus(@PathVariable UUID orderId) {
        Object response = orderService.getOrderStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
