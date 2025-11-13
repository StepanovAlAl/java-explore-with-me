package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.model.Category;
import ru.practicum.model.Location;
import ru.practicum.model.enums.EventState;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.service.EventService;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatsService statsService;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (newEventDto.getEventDate() == null) {
            throw new ValidationException("Event date cannot be null");
        }
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date too soon");
        }

        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .initiator(user)
                .location(Location.builder()
                        .lat(newEventDto.getLocation().getLat())
                        .lon(newEventDto.getLocation().getLon())
                        .build())
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .title(newEventDto.getTitle())
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0)
                .views(0L)
                .build();

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }

        return eventRepository.findByInitiatorId(userId, pageable).stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return eventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date too soon");
        }

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            if ("SEND_TO_REVIEW".equals(updateRequest.getStateAction())) {
                event.setState(EventState.PENDING);
            } else if ("CANCEL_REVIEW".equals(updateRequest.getStateAction())) {
                event.setState(EventState.CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Pageable pageable) {
        List<EventState> eventStates = null;
        if (states != null) {
            eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        List<Event> events = eventRepository.findEventsByAdmin(users, eventStates, categories, rangeStart, rangeEnd, pageable);
        return events.stream()
                .map(eventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (updateRequest.getStateAction() != null) {
            if ("PUBLISH_EVENT".equals(updateRequest.getStateAction())) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Event not pending");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Event date too soon");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if ("REJECT_EVENT".equals(updateRequest.getStateAction())) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Event already published");
                }
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Pageable pageable) {
        try {
            if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
                throw new ValidationException("Start date after end date");
            }

            if (rangeStart == null) {
                rangeStart = LocalDateTime.now();
            }

            List<Event> events = eventRepository.findEventsPublic(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable);

            for (Event event : events) {
                try {
                    Long views = statsService.getEventViews(event.getId());
                    event.setViews(views != null ? views : 0L);
                } catch (Exception e) {
                    log.warn("Failed to get views for event {}: {}", event.getId(), e.getMessage());
                    event.setViews(0L);
                }
            }

            if (sort != null) {
                if ("EVENT_DATE".equals(sort)) {
                    events.sort((e1, e2) -> e1.getEventDate().compareTo(e2.getEventDate()));
                } else if ("VIEWS".equals(sort)) {
                    events.sort((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()));
                }
            }

            return events.stream()
                    .map(eventMapper::toEventShortDto)
                    .collect(Collectors.toList());

        } catch (ValidationException e) {
            throw e; // ← Перебросить валидационные ошибки
        } catch (Exception e) {
            log.error("Error getting public events", e);
            throw new RuntimeException("Failed to get events", e);
        }
    }

    @Override
    public EventFullDto getEventPublic(Long eventId) {
        try {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Event not found"));

            if (event.getState() != EventState.PUBLISHED) {
                throw new NotFoundException("Event not found");
            }

            Long views = statsService.getEventViews(eventId);
            event.setViews(views);

            Event savedEvent = eventRepository.save(event);
            return eventMapper.toEventFullDto(savedEvent);

        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting public event: {}", eventId, e);
            throw new RuntimeException("Failed to get event");
        }
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        updateCommonFields(event, updateRequest);
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest updateRequest) {
        updateCommonFields(event, updateRequest);
    }

    private void updateCommonFields(Event event, Object updateRequest) {
        if (updateRequest instanceof UpdateEventUserRequest) {
            UpdateEventUserRequest request = (UpdateEventUserRequest) updateRequest;
            updateFields(event, request.getAnnotation(), request.getCategory(), request.getDescription(),
                    request.getEventDate(), request.getLocation(), request.getPaid(),
                    request.getParticipantLimit(), request.getRequestModeration(), request.getTitle());
        } else if (updateRequest instanceof UpdateEventAdminRequest) {
            UpdateEventAdminRequest request = (UpdateEventAdminRequest) updateRequest;
            updateFields(event, request.getAnnotation(), request.getCategory(), request.getDescription(),
                    request.getEventDate(), request.getLocation(), request.getPaid(),
                    request.getParticipantLimit(), request.getRequestModeration(), request.getTitle());
        }
    }

    private void updateFields(Event event, String annotation, Long categoryId, String description,
                              LocalDateTime eventDate, LocationDto location, Boolean paid,
                              Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (eventDate != null) {
            event.setEventDate(eventDate);
        }
        if (location != null) {
            event.setLocation(Location.builder()
                    .lat(location.getLat())
                    .lon(location.getLon())
                    .build());
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (title != null) {
            event.setTitle(title);
        }
    }
}
