package ru.practicum.service.impl;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsClient statsClient;

    @Override
    public void saveHit(HttpServletRequest request) {
        try {
            String uri = request.getRequestURI();
            if (uri.startsWith("/events")) {
                statsClient.hit(request, "ewm-main-service");
                log.debug("Hit saved for URI: {}", uri);
            }
        } catch (Exception e) {
            log.warn("Failed to save hit to stats service: {}", e.getMessage());
        }
    }

    @Override
    public Long getEventViews(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(10);
            LocalDateTime end = LocalDateTime.now().plusHours(1);

            String eventUri = "/events/" + eventId;
            var stats = statsClient.getStats(start, end, List.of(eventUri), true);

            if (stats != null && !stats.isEmpty()) {
                for (var stat : stats) {
                    if (eventUri.equals(stat.getUri())) {
                        Long views = stat.getHits();
                        log.debug("Found {} views for eventId: {}", views, eventId);
                        return views;
                    }
                }
            }
            log.debug("No stats found for eventId: {}", eventId);
            return 0L;
        } catch (Exception e) {
            log.warn("Error getting event views for eventId: {}, error: {}", eventId, e.getMessage());
            return 0L;
        }
    }
}
