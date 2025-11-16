package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
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

import jakarta.persistence.criteria.Predicate;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + newEventDto.getCategory() + " was not found"));

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least 2 hours from now");
        }

        Event event = EventMapper.toEvent(newEventDto, user, category);
        Event savedEvent = eventRepository.save(event);
        log.info("Created event with id: {} for user: {}", savedEvent.getId(), userId);

        return EventMapper.toEventFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        Page<Event> eventsPage = eventRepository.findByInitiatorId(userId, pageable);
        List<Event> events = eventsPage.getContent();
        Map<Long, Long> viewsMap = getEventsViews(events);

        return events.stream()
                .map(EventMapper::toEventShortDto)
                .map(dto -> {
                    dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        EventFullDto dto = EventMapper.toEventFullDto(event);
        Long views = getEventViewsFromStats(eventId);
        dto.setViews(views);
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null && updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least 2 hours from now");
        }

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setState(EventState.PENDING);
                    break;
                case "CANCEL_REVIEW":
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ValidationException("Invalid state action: " + updateRequest.getStateAction());
            }
        }

        Event updatedEvent = eventRepository.save(event);
        EventFullDto result = EventMapper.toEventFullDto(updatedEvent);
        result.setViews(getEventViewsFromStats(eventId));
        return result;
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Pageable pageable) {

        PublicEventParams params = new PublicEventParams(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, 0, 10);
        return getPublicEvents(params);
    }

    @Override
    public EventFullDto getEventPublic(Long id) {
        log.info("Getting public event with id: {}", id);

        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        Long views = getEventViewsFromStats(id);
        log.info("Event {} has {} views from stats service", id, views);

        EventFullDto dto = EventMapper.toEventFullDto(event);
        dto.setViews(views);

        log.info("Returning event {} with {} views", id, views);
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto publishEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PENDING) {
            throw new ConflictException("Only pending events can be published");
        }

        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Event date must be at least 1 hour from now");
        }

        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);
        EventFullDto result = EventMapper.toEventFullDto(savedEvent);
        result.setViews(getEventViewsFromStats(eventId));
        return result;
    }

    @Override
    @Transactional
    public EventFullDto rejectEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Published events cannot be rejected");
        }

        event.setState(EventState.CANCELED);
        Event savedEvent = eventRepository.save(event);
        EventFullDto result = EventMapper.toEventFullDto(savedEvent);
        result.setViews(getEventViewsFromStats(eventId));
        return result;
    }

    @Override
    public List<EventFullDto> getEventsAdmin(List<Long> users, List<String> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Pageable pageable) {

        AdminEventParams params = new AdminEventParams(users, states, categories, rangeStart, rangeEnd, 0, 10);
        return getAdminEvents(params);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (updateRequest.getEventDate() != null && updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Event date must be at least 1 hour from now");
        }

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case "PUBLISH_EVENT":
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Only pending events can be published");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case "REJECT_EVENT":
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Published events cannot be rejected");
                    }
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ValidationException("Invalid state action: " + updateRequest.getStateAction());
            }
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);
        EventFullDto result = EventMapper.toEventFullDto(updatedEvent);
        result.setViews(getEventViewsFromStats(eventId));
        return result;
    }

    @Override
    public List<EventFullDto> getAdminEvents(AdminEventParams params) {
        List<EventState> eventStates = convertToEventStates(params.getStates());

        Specification<Event> spec = buildAdminEventSpecification(
                params.getUsers(), eventStates, params.getCategories(),
                params.getRangeStart(), params.getRangeEnd()
        );

        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        Page<Event> eventsPage = eventRepository.findAll(spec, pageable);

        Map<Long, Long> viewsMap = getEventsViews(eventsPage.getContent());

        return eventsPage.getContent().stream()
                .map(EventMapper::toEventFullDto)
                .map(dto -> {
                    dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<EventShortDto> getPublicEvents(PublicEventParams params) {
        log.info("Getting public events with params: {}", params);

        LocalDateTime rangeStart = params.getRangeStart() != null ? params.getRangeStart() : LocalDateTime.now();
        LocalDateTime rangeEnd = params.getRangeEnd() != null ? params.getRangeEnd() : LocalDateTime.now().plusYears(100);

        if (rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Start date must be before end date");
        }

        Specification<Event> spec = buildPublicEventSpecification(
                params.getText(), params.getCategories(), params.getPaid(),
                rangeStart, rangeEnd, params.getOnlyAvailable()
        );

        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize(), getSort(params.getSort()));
        Page<Event> eventsPage = eventRepository.findAll(spec, pageable);

        Map<Long, Long> viewsMap = getEventsViews(eventsPage.getContent());

        List<EventShortDto> result = eventsPage.getContent().stream()
                .map(EventMapper::toEventShortDto)
                .map(dto -> {
                    dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Returning {} public events", result.size());
        return result;
    }

    private Specification<Event> buildPublicEventSpecification(String text, List<Long> categories, Boolean paid,
                                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                               Boolean onlyAvailable) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

            if (text != null && !text.isEmpty()) {
                String likePattern = "%" + text.toLowerCase() + "%";
                Predicate annotationLike = cb.like(cb.lower(root.get("annotation")), likePattern);
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), likePattern);
                predicates.add(cb.or(annotationLike, descriptionLike));
            }

            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }

            if (paid != null) {
                predicates.add(cb.equal(root.get("paid"), paid));
            }

            predicates.add(cb.between(root.get("eventDate"), rangeStart, rangeEnd));

            if (Boolean.TRUE.equals(onlyAvailable)) {
                Predicate noLimit = cb.equal(root.get("participantLimit"), 0);
                Predicate hasSpace = cb.lessThan(root.get("confirmedRequests"), root.get("participantLimit"));
                predicates.add(cb.or(noLimit, hasSpace));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Event> buildAdminEventSpecification(List<Long> users, List<EventState> states,
                                                              List<Long> categories, LocalDateTime rangeStart,
                                                              LocalDateTime rangeEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (users != null && !users.isEmpty()) {
                predicates.add(root.get("initiator").get("id").in(users));
            }

            if (states != null && !states.isEmpty()) {
                predicates.add(root.get("state").in(states));
            }

            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }

            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<EventState> convertToEventStates(List<String> stateStrings) {
        if (stateStrings == null || stateStrings.isEmpty()) {
            return null;
        }

        List<EventState> eventStates = new ArrayList<>();
        for (String state : stateStrings) {
            try {
                eventStates.add(EventState.valueOf(state.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid state: " + state);
            }
        }
        return eventStates;
    }

    private Sort getSort(String sort) {
        if ("EVENT_DATE".equals(sort)) {
            return Sort.by("eventDate").ascending();
        } else if ("VIEWS".equals(sort)) {
            return Sort.by("views").descending();
        }
        return Sort.by("id").ascending();
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null && !updateRequest.getAnnotation().isBlank()) {
            event.setAnnotation(updateRequest.getAnnotation());
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + updateRequest.getCategory() + " was not found"));
            event.setCategory(category);
        }

        if (updateRequest.getDescription() != null && !updateRequest.getDescription().isBlank()) {
            event.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getLocation() != null) {
            Location location = new Location();
            location.setLat(updateRequest.getLocation().getLat());
            location.setLon(updateRequest.getLocation().getLon());
            event.setLocation(location);
        }

        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }

        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }

        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }

        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getAnnotation() != null && !updateRequest.getAnnotation().isBlank()) {
            event.setAnnotation(updateRequest.getAnnotation());
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + updateRequest.getCategory() + " was not found"));
            event.setCategory(category);
        }

        if (updateRequest.getDescription() != null && !updateRequest.getDescription().isBlank()) {
            event.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getLocation() != null) {
            Location location = new Location();
            location.setLat(updateRequest.getLocation().getLat());
            location.setLon(updateRequest.getLocation().getLon());
            event.setLocation(location);
        }

        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }

        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }

        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }

        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private Long getEventViewsFromStats(Long eventId) {
        try {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

            LocalDateTime start = event.getPublishedOn() != null ? event.getPublishedOn() : LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = List.of("/events/" + eventId);

            log.debug("Getting stats for event {}: start={}, end={}, uris={}", eventId, start, end, uris);

            var stats = statsClient.getStats(start, end, uris, true);

            Long views = 0L;
            for (var stat : stats) {
                if (("/events/" + eventId).equals(stat.getUri())) {
                    views = stat.getHits() != null ? stat.getHits() : 0L;
                    break;
                }
            }

            log.debug("Event {} has {} views from stats service", eventId, views);
            return views;

        } catch (Exception e) {
            log.warn("Error getting event views from stats for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private Map<Long, Long> getEventsViews(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        try {
            List<String> uris = events.stream()
                    .map(event -> "/events/" + event.getId())
                    .collect(Collectors.toList());

            LocalDateTime start = events.stream()
                    .map(event -> event.getPublishedOn() != null ? event.getPublishedOn() : LocalDateTime.now().minusYears(1))
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now().minusYears(1));
            LocalDateTime end = LocalDateTime.now();

            var stats = statsClient.getStats(start, end, uris, true);

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> extractEventIdFromUri(stat.getUri()),
                            stat -> stat.getHits() != null ? stat.getHits() : 0L,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.warn("Error getting events views: {}", e.getMessage());
            return events.stream()
                    .collect(Collectors.toMap(Event::getId, event -> 0L));
        }
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("Error extracting event ID from URI: {}", uri);
            return 0L;
        }
    }
}
