package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.service.StatsService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class StatsController {

    private final StatsService service;

    @PostMapping("/hit")
    @ResponseStatus(code = HttpStatus.CREATED)
    public void hit(@Valid @RequestBody EndpointHit hit) {
        log.info("POST request to save information.");
        service.saveHit(hit);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        log.info("GET request to get all statistic. start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        if (start == null) {
            throw new IllegalArgumentException("Parameter 'start' is required");
        }

        if (end == null) {
            throw new IllegalArgumentException("Parameter 'end' is required");
        }

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        return service.getStats(start, end, uris, unique);
    }
}
