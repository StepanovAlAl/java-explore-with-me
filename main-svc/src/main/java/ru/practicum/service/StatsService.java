package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;

public interface StatsService {
    void saveHit(HttpServletRequest request);

    Long getEventViews(Long eventId);
}