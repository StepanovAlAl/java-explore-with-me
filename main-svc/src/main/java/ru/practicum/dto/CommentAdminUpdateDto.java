package ru.practicum.dto;

import lombok.Data;

@Data
public class CommentAdminUpdateDto {
    private String status;
    private Boolean pinned;
}