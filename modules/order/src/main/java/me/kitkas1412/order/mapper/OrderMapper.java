package me.kitkas1412.order.mapper;

import me.kitkas1412.common.dto.response.BuyTicketAcceptedResponse;
import me.kitkas1412.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Khai báo mapping từ Order sang BuyTicketAcceptedResponse.
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "eventId", source = "event_id")
    @Mapping(target = "status", source = "status")
    BuyTicketAcceptedResponse toBuyTicketAcceptedResponse(Order order);
}
