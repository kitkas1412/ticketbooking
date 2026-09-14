package me.kitkas1412.event.mapper;

import me.kitkas1412.event.dto.response.EventResponse;
import me.kitkas1412.event.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Map Event sang response DTO, đổi id thành eventId và status thành eventStatus.
 */
@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "eventId", source = "id")
    @Mapping(target = "eventStatus", source = "status")
    EventResponse toResponse(Event event);
}
