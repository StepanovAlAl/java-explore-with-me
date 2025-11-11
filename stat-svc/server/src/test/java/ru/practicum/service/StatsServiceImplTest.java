package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.model.EndpointHitEntity;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    @Test
    void saveHit_ShouldSaveEntity() {
        EndpointHit hit = new EndpointHit(null, "app", "/uri", "192.168.1.1", LocalDateTime.now());

        statsService.saveHit(hit);

        verify(statsRepository, times(1)).save(any(EndpointHitEntity.class));
    }

    @Test
    void getStats_WhenUniqueFalse_ShouldCallFindStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/events/1");

        when(statsRepository.findStats(start, end, uris))
                .thenReturn(List.of(new ViewStats("app", "/events/1", 5L)));

        List<ViewStats> result = statsService.getStats(start, end, uris, false);

        assertEquals(1, result.size());
        verify(statsRepository, times(1)).findStats(start, end, uris);
        verify(statsRepository, never()).findUniqueStats(any(), any(), any());
    }

    @Test
    void getStats_WhenUniqueTrue_ShouldCallFindUniqueStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/events/1");

        when(statsRepository.findUniqueStats(start, end, uris))
                .thenReturn(List.of(new ViewStats("app", "/events/1", 3L)));

        List<ViewStats> result = statsService.getStats(start, end, uris, true);

        assertEquals(1, result.size());
        verify(statsRepository, times(1)).findUniqueStats(start, end, uris);
        verify(statsRepository, never()).findStats(any(), any(), any());
    }

    @Test
    void getStats_WhenStartAfterEnd_ShouldThrowException() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        assertThrows(IllegalArgumentException.class,
                () -> statsService.getStats(start, end, null, false));
    }
}