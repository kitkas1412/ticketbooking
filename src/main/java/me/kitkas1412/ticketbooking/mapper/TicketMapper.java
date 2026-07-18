package me.kitkas1412.ticketbooking.mapper;

import me.kitkas1412.ticketbooking.dto.response.TicketResponse;
import me.kitkas1412.ticketbooking.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TicketMapper {

    @Mapping(target = "eventId", source = "event.eventId")
    @Mapping(target = "orderId", source = "order.orderId")
    TicketResponse toResponse(Ticket ticket);
}
