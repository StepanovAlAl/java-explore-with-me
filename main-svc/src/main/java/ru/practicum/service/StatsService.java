package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

public interface StatsService {
    void saveHit(HttpServletRequest request);

    Long getEventViews(Long eventId);
}