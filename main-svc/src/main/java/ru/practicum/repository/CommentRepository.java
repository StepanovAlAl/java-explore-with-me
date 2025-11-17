package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Comment;
import ru.practicum.model.enums.CommentStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdAndStatusAndParentCommentIsNull(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findByAuthorId(Long userId, Pageable pageable);

    List<Comment> findByStatus(CommentStatus status, Pageable pageable);

    Optional<Comment> findByIdAndAuthorId(Long commentId, Long userId);

    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId AND c.status = 'APPROVED' " +
            "ORDER BY c.pinned DESC, c.rating DESC, c.created DESC")
    List<Comment> findTopByEventIdOrderByRating(@Param("eventId") Long eventId, Pageable pageable);

    @Query("SELECT c.event.id, COUNT(c) FROM Comment c " +
            "WHERE c.event.id IN :eventIds AND c.status = :status AND c.parentComment IS NULL " +
            "GROUP BY c.event.id")
    List<Object[]> countCommentsByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds,
                                                    @Param("status") CommentStatus status);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.event.id = :eventId AND c.status = :status AND c.parentComment IS NULL")
    Long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") CommentStatus status);
}