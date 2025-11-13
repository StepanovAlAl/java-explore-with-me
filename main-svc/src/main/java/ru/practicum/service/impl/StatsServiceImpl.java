package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsClient statsClient;

    @Override
    public void saveHit(HttpServletRequest request) {
        statsClient.hit(request, "ewm-main-service");
    }

    @Override
    public Long getEventViews(Long eventId) {
        LocalDateTime start = LocalDateTime.now().minusYears(1);
        LocalDateTime end = LocalDateTime.now();

        var stats = statsClient.getStats(start, end, List.of("/events/" + eventId), false);

        if (!stats.isEmpty()) {
            return stats.get(0).getHits();
        }
        return 0L;
    }
}
