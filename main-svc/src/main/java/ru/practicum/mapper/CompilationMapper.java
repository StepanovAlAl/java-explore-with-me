package ru.practicum.mapper;

import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.model.Compilation;

import java.util.Map;
import java.util.stream.Collectors;

public class CompilationMapper {

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

    public static CompilationDto toCompilationDto(Compilation compilation, Map<Long, Integer> eventCommentsCount) {
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
                        Integer commentsCount = eventCommentsCount != null ?
                                eventCommentsCount.getOrDefault(event.getId(), 0) : 0;
                        EventShortDto shortDto = EventMapper.toEventShortDto(event, commentsCount);
                        return shortDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
