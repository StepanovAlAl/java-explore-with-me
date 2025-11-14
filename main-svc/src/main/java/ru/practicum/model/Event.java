package ru.practicum.model;

import lombok.*;
import ru.practicum.model.enums.EventState;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 20, max = 2000)
    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotBlank
    @Size(min = 20, max = 7000)
    @Column(name = "description", nullable = false, length = 7000)
    private String description;

    @NotNull
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @NotNull
    @Embedded
    private Location location;

    @NotNull
    @Column(name = "paid", nullable = false)
    private Boolean paid;

    @NotNull
    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit;

    @NotNull
    @Column(name = "request_moderation", nullable = false)
    private Boolean requestModeration;

    @NotBlank
    @Size(min = 3, max = 120)
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @NotNull
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private EventState state;

    @NotNull
    @Column(name = "confirmed_requests", nullable = false)
    private Integer confirmedRequests;

    @NotNull
    @Column(name = "views", nullable = false)
    private Long views;
}