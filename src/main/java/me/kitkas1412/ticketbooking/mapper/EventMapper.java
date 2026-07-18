package me.kitkas1412.ticketbooking.mapper;

import me.kitkas1412.ticketbooking.dto.response.EventResponse;
import me.kitkas1412.ticketbooking.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "eventId", source = "id")
    @Mapping(target = "eventStatus", source = "status")
    EventResponse toResponse(Event event);
}
