package ru.practicum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class NewCommentDto {
    @NotBlank
    @Size(min = 1, max = 2000)
    private String text;

    private Long parentCommentId;
}