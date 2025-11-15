package ru.practicum.dto;

import lombok.Data;
import ru.practicum.model.enums.RequestStatus;
import java.time.LocalDateTime;

@Data
public class ParticipationRequestDto {
    private Long id;
    private LocalDateTime created;
    private Long event;
    private Long requester;
    private RequestStatus status;
}