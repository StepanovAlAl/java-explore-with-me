package ru.practicum.service;

import ru.practicum.dto.*;
import org.springframework.data.domain.Pageable;
import ru.practicum.model.enums.CommentStatus;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteComment(Long userId, Long commentId);

    List<CommentDto> getUserComments(Long userId, Pageable pageable);

    List<CommentDto> getEventComments(Long eventId, String sort, Pageable pageable);

    List<CommentDto> getCommentsForModeration(CommentStatus status, Pageable pageable);

    CommentDto moderateComment(Long commentId, CommentAdminUpdateDto updateDto);

    CommentDto likeComment(Long userId, Long commentId);

    CommentDto dislikeComment(Long userId, Long commentId);

    void removeLike(Long userId, Long commentId);
}