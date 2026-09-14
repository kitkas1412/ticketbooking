package me.kitkas1412.order.mapper;

import me.kitkas1412.common.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.order.entity.Order;
import org.mapstruct.Mapping;

/**
 * Khai báo mapping từ Order sang BuyTicketAcceptedResponse.
 * Mapping event.id vẫn theo quan hệ entity cũ của Order.
 */
public interface OrderMapper {

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "status", source = "status")
    BuyTicketAcceptedResponse toBuyTicketAcceptedResponse(Order order);
}
