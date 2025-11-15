package ru.practicum.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.AdminEventParams;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.UpdateEventAdminRequest;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {

        if (from < 0) {
            throw new IllegalArgumentException("Parameter 'from' must be greater than or equal to 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Parameter 'size' must be greater than 0");
        }

        AdminEventParams params = new AdminEventParams(users, states, categories, rangeStart, rangeEnd, from, size);
        return eventService.getAdminEvents(params);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long eventId,
                                    @RequestBody UpdateEventAdminRequest updateRequest) {
        return eventService.updateEventByAdmin(eventId, updateRequest);
    }

    @PatchMapping("/{eventId}/publish")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto publishEvent(@PathVariable Long eventId) {
        return eventService.publishEvent(eventId);
    }

    @PatchMapping("/{eventId}/reject")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto rejectEvent(@PathVariable Long eventId) {
        return eventService.rejectEvent(eventId);
    }
}