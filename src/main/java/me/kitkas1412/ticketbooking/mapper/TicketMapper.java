package me.kitkas1412.ticketbooking.mapper;

import me.kitkas1412.ticketbooking.dto.response.BuyTicketResponse;
import me.kitkas1412.ticketbooking.entity.Order;
import me.kitkas1412.ticketbooking.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "seatCode", source = "ticket.seatCode")
    @Mapping(target = "status", source = "ticket.status")
    @Mapping(target = "eventId", source = "ticket.event.id")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "price", source = "ticket.price")
    BuyTicketResponse toBuyTicketResponse(Ticket ticket, Order order);
}
