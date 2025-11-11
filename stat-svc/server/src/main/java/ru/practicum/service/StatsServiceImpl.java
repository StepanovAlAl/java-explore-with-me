package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.model.EndpointHitEntity;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHit endpointHit) {
        EndpointHitEntity entity = new EndpointHitEntity();
        entity.setApp(endpointHit.getApp());
        entity.setUri(endpointHit.getUri());
        entity.setIp(endpointHit.getIp());
        entity.setTimestamp(endpointHit.getTimestamp());

        statsRepository.save(entity);
        log.info("Saved hit: app={}, uri={}, ip={}", entity.getApp(), entity.getUri(), entity.getIp());
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Getting stats from {} to {} for uris: {}, unique: {}", start, end, uris, unique);

        validateTimeRange(start, end);

        List<ViewStats> stats;
        if (Boolean.TRUE.equals(unique)) {
            stats = statsRepository.findUniqueStats(start, end, uris);
        } else {
            stats = statsRepository.findStats(start, end, uris);
        }

        log.info("Found {} stats records", stats.size());
        return stats;
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }
}