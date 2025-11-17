package ru.practicum.mapper;

import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.model.Compilation;
import ru.practicum.repository.CommentRepository;
import ru.practicum.model.enums.CommentStatus;

import java.util.stream.Collectors;

public class CompilationMapper {

    private static CommentRepository commentRepository;

    public static void setCommentRepository(CommentRepository repository) {
        commentRepository = repository;
    }

    public static Compilation toCompilation(NewCompilationDto newCompilationDto) {
        if (newCompilationDto == null) {
            return null;
        }

        Compilation compilation = new Compilation();
        compilation.setTitle(newCompilationDto.getTitle());
        compilation.setPinned(newCompilationDto.getPinned());
        return compilation;
    }

    public static CompilationDto toCompilationDto(Compilation compilation) {
        return toCompilationDto(compilation, null);
    }

    public static CompilationDto toCompilationDto(Compilation compilation, CommentRepository commentRepo) {
        if (compilation == null) {
            return null;
        }

        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setTitle(compilation.getTitle());
        dto.setPinned(compilation.getPinned());

        if (compilation.getEvents() != null) {
            dto.setEvents(compilation.getEvents().stream()
                    .map(event -> {
                        Long commentsCount = commentRepo != null ?
                                commentRepo.countByEventIdAndStatus(event.getId(), CommentStatus.APPROVED) : 0L;
                        EventShortDto shortDto = EventMapper.toEventShortDto(event,
                                commentsCount != null ? commentsCount.intValue() : 0);
                        return shortDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
