package ru.practicum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class NewCompilationDto {
    private List<Long> events;
    private Boolean pinned;

    @NotBlank
    @Size(min = 1, max = 50)
    private String title;
}