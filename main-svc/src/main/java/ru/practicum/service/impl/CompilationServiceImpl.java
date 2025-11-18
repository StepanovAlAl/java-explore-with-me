package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.model.enums.CommentStatus;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.CommentRepository;
import ru.practicum.service.CompilationService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findByIdIn(newCompilationDto.getEvents());
            Set<Event> eventSet = new HashSet<>(events);
            compilation.setEvents(eventSet);
        } else {
            compilation.setEvents(new HashSet<>());
        }

        if (compilation.getPinned() == null) {
            compilation.setPinned(false);
        }

        Compilation savedCompilation = compilationRepository.save(compilation);

        Map<Long, Integer> commentsCountMap = getCommentsCountForEvents(savedCompilation.getEvents());

        return CompilationMapper.toCompilationDto(savedCompilation, commentsCountMap);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getEvents() != null) {
            List<Event> events = eventRepository.findByIdIn(updateRequest.getEvents());
            Set<Event> eventSet = new HashSet<>(events);
            compilation.setEvents(eventSet);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);

        Map<Long, Integer> commentsCountMap = getCommentsCountForEvents(updatedCompilation.getEvents());

        return CompilationMapper.toCompilationDto(updatedCompilation, commentsCountMap);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable) {
        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        Set<Event> allEvents = compilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream())
                .collect(Collectors.toSet());

        Map<Long, Integer> commentsCountMap = getCommentsCountForEvents(allEvents);

        return compilations.stream()
                .map(compilation -> CompilationMapper.toCompilationDto(compilation, commentsCountMap))
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        Map<Long, Integer> commentsCountMap = getCommentsCountForEvents(compilation.getEvents());

        return CompilationMapper.toCompilationDto(compilation, commentsCountMap);
    }

    private Map<Long, Integer> getCommentsCountForEvents(Set<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        List<Object[]> commentsCounts = commentRepository.countCommentsByEventIdsAndStatus(
                eventIds, CommentStatus.APPROVED);

        return commentsCounts.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> ((Long) result[1]).intValue(),
                        (existing, replacement) -> existing
                ));
    }
}
