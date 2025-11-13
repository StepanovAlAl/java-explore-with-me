package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class UpdateEventUserRequest {
    @Size(min = 20, max = 2000)
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000)
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private LocationDto location;

    private Boolean paid;

    @PositiveOrZero
    private Integer participantLimit;

    private Boolean requestModeration;

    private String stateAction; // SEND_TO_REVIEW, CANCEL_REVIEW

    @Size(min = 3, max = 120)
    private String title;
}