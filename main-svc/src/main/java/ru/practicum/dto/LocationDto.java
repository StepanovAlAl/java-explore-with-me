package ru.practicum.dto;

import lombok.Data;
import jakarta.persistence.Embeddable;

@Data
@Embeddable
public class LocationDto {
    private Float lat;
    private Float lon;
}