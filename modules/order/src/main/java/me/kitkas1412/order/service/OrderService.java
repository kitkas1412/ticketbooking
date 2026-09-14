package me.kitkas1412.order.service;


import me.kitkas1412.common.dto.request.BuyTicketRequest;
import me.kitkas1412.common.dto.response.BuyTicketAcceptedResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface tiếp nhận đặt vé; trả Optional.empty() nếu idempotency key đã có trong Redis.
 */
public interface OrderService {
    Optional<BuyTicketAcceptedResponse> buyTicket(BuyTicketRequest request, UUID eventId) throws Exception;

//    Object getOrderStatus(UUID orderId);
}
