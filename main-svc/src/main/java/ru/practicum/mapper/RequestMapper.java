package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.model.ParticipationRequest;

@Component
public class RequestMapper {

    public ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }
        ParticipationRequestDto requestDto = new ParticipationRequestDto();
        requestDto.setId(request.getId());
        requestDto.setCreated(request.getCreated());
        requestDto.setStatus(request.getStatus());

        if (request.getEvent() != null) {
            requestDto.setEvent(request.getEvent().getId());
        }

        if (request.getRequester() != null) {
            requestDto.setRequester(request.getRequester().getId());
        }

        return requestDto;
    }
}
