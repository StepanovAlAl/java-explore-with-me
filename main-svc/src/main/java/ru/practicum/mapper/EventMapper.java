package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.model.Event;
import ru.practicum.model.Location;

@Component
public class EventMapper {

    public Event toEvent(NewEventDto newEventDto) {
        if (newEventDto == null) {
            return null;
        }

        Event event = new Event();
        event.setAnnotation(newEventDto.getAnnotation());
        event.setDescription(newEventDto.getDescription());
        event.setEventDate(newEventDto.getEventDate());

        if (newEventDto.getLocation() != null) {
            Location location = new Location();
            location.setLat(newEventDto.getLocation().getLat());
            location.setLon(newEventDto.getLocation().getLon());
            event.setLocation(location);
        }

        event.setPaid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false);
        event.setParticipantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0);
        event.setRequestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true);
        event.setTitle(newEventDto.getTitle());

        return event;
    }

    public EventFullDto toEventFullDto(Event event) {
        if (event == null) {
            return null;
        }

        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());

        if (event.getLocation() != null) {
            ru.practicum.dto.LocationDto locationDto = new ru.practicum.dto.LocationDto();
            locationDto.setLat(event.getLocation().getLat());
            locationDto.setLon(event.getLocation().getLon());
            dto.setLocation(locationDto);
        }

        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setTitle(event.getTitle());
        dto.setCreatedOn(event.getCreatedOn());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setState(event.getState());
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setViews(event.getViews());

        if (event.getInitiator() != null) {
            dto.setInitiator(new UserMapper().toUserShortDto(event.getInitiator()));
        }

        if (event.getCategory() != null) {
            dto.setCategory(new CategoryMapper().toCategoryDto(event.getCategory()));
        }

        return dto;
    }

    public EventShortDto toEventShortDto(Event event) {
        if (event == null) {
            return null;
        }

        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setEventDate(event.getEventDate());
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setViews(event.getViews());

        if (event.getInitiator() != null) {
            dto.setInitiator(new UserMapper().toUserShortDto(event.getInitiator()));
        }

        if (event.getCategory() != null) {
            dto.setCategory(new CategoryMapper().toCategoryDto(event.getCategory()));
        }

        return dto;
    }
}
