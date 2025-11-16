package ru.practicum.mapper;

import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.model.Event;
import ru.practicum.model.Location;
import ru.practicum.model.User;
import ru.practicum.model.Category;
import ru.practicum.model.enums.EventState;

import java.time.LocalDateTime;

public class EventMapper {

    public static Event toEvent(NewEventDto newEventDto, User user, Category category) {
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

        event.setInitiator(user);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        event.setConfirmedRequests(0);

        return event;
    }

    public static EventFullDto toEventFullDto(Event event) {
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
        dto.setViews(0L);

        if (event.getInitiator() != null) {
            dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
        }

        if (event.getCategory() != null) {
            dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        return dto;
    }

    public static EventShortDto toEventShortDto(Event event) {
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
        dto.setViews(0L);

        if (event.getInitiator() != null) {
            dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
        }

        if (event.getCategory() != null) {
            dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        return dto;
    }
}