package me.kitkas1412.ticketbooking.mapper;

import me.kitkas1412.ticketbooking.dto.response.EventResponse;
import me.kitkas1412.ticketbooking.entity.Event;
import org.mapstruct.Mapper;

@Mapper
public interface EventMapper {
    EventResponse toResponse(Event event);
}
