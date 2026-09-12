package me.kitkas1412.order.mapper;

import me.kitkas1412.common.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.order.entity.Order;
import org.mapstruct.Mapping;

public interface OrderMapper {

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "status", source = "status")
    BuyTicketAcceptedResponse toBuyTicketAcceptedResponse(Order order);
}
