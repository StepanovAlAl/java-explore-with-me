package ru.practicum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Data
public class CommentDto {
    private Long id;

    @NotBlank
    @Size(min = 1, max = 2000)
    private String text;

    private UserShortDto author;
    private Long eventId;
    private Long parentCommentId;
    private String status;
    private LocalDateTime created;
    private LocalDateTime updated;
    private Integer likesCount;
    private Integer dislikesCount;
    private Integer rating;
    private Boolean pinned;
}