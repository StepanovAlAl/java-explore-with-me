package ru.practicum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class NewCategoryDto {
    @NotBlank
    @Size(min = 1, max = 50)
    private String name;
}