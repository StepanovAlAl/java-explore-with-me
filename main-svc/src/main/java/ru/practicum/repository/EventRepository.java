package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;
import ru.practicum.model.enums.EventState;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Optional<Event> findByIdAndState(Long eventId, EventState state);

    boolean existsByCategoryId(Long categoryId);

    List<Event> findByIdIn(List<Long> eventIds);

    @Query("SELECT e FROM Event e WHERE e.id = :eventId AND e.initiator.id = :userId")
    Optional<Event> findByEventIdAndInitiatorId(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("SELECT e FROM Event e WHERE e.initiator.id = :userId AND e.id = :eventId")
    Optional<Event> findByInitiatorIdAndEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);


}
