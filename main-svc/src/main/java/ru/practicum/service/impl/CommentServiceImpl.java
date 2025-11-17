package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.*;
import ru.practicum.model.enums.CommentStatus;
import ru.practicum.model.enums.CommentSort;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.enums.LikeType;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.CommentLikeRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment on unpublished event");
        }

        Comment parentComment = null;
        if (newCommentDto.getParentCommentId() != null) {
            parentComment = commentRepository.findById(newCommentDto.getParentCommentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment with id=" + newCommentDto.getParentCommentId() + " was not found"));

            if (!parentComment.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Parent comment belongs to different event");
            }
        }

        Comment comment = Comment.builder()
                .text(newCommentDto.getText())
                .author(user)
                .event(event)
                .parentComment(parentComment)
                .status(CommentStatus.PENDING)
                .created(LocalDateTime.now())
                .likesCount(0)
                .dislikesCount(0)
                .rating(0)
                .pinned(false)
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Created comment with id: {} for event: {} by user: {}", savedComment.getId(), eventId, userId);

        return CommentMapper.toCommentDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new ConflictException("Cannot update deleted comment");
        }

        if (comment.getCreated().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new ConflictException("Comment can only be edited within 24 hours of creation");
        }

        comment.setText(updateCommentDto.getText());
        comment.setUpdated(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);

        Comment updatedComment = commentRepository.save(comment);
        return CommentMapper.toCommentDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        if (!comment.getReplies().isEmpty()) {
            for (Comment reply : comment.getReplies()) {
                reply.setText("[deleted - parent comment was deleted]");
                reply.setStatus(CommentStatus.DELETED);
                reply.setUpdated(LocalDateTime.now());
                commentRepository.save(reply);
            }
        }

        comment.setText("[deleted]");
        comment.setStatus(CommentStatus.DELETED);
        comment.setUpdated(LocalDateTime.now());

        commentRepository.save(comment);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        return commentRepository.findByAuthorId(userId, pageable).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, String sort, Pageable pageable) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        List<Comment> comments;
        CommentSort sortType = parseSortType(sort);

        if (sortType == CommentSort.RATING) {
            comments = commentRepository.findTopByEventIdOrderByRating(eventId, pageable);
        } else {
            comments = commentRepository.findByEventIdAndStatusAndParentCommentIsNull(eventId, CommentStatus.APPROVED, pageable);
        }

        return comments.stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsForModeration(CommentStatus status, Pageable pageable) {
        return commentRepository.findByStatus(status, pageable).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, CommentAdminUpdateDto updateDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        if (updateDto.getStatus() != null) {
            CommentStatus newStatus = CommentStatus.from(updateDto.getStatus());
            comment.setStatus(newStatus);
        }

        if (updateDto.getPinned() != null) {
            comment.setPinned(updateDto.getPinned());
        }

        comment.setUpdated(LocalDateTime.now());
        Comment updatedComment = commentRepository.save(comment);
        return CommentMapper.toCommentDto(updatedComment);
    }

    @Override
    @Transactional
    public CommentDto likeComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        if (comment.getStatus() != CommentStatus.APPROVED) {
            throw new ConflictException("Cannot like unapproved comment");
        }

        return handleLike(userId, commentId, LikeType.LIKE);
    }

    @Override
    @Transactional
    public CommentDto dislikeComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        if (comment.getStatus() != CommentStatus.APPROVED) {
            throw new ConflictException("Cannot dislike unapproved comment");
        }

        return handleLike(userId, commentId, LikeType.DISLIKE);
    }

    @Override
    @Transactional
    public void removeLike(Long userId, Long commentId) {
        CommentLike existingLike = commentLikeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new NotFoundException("Like not found for comment id=" + commentId));

        Comment comment = existingLike.getComment();

        if (existingLike.getType() == LikeType.LIKE) {
            comment.setLikesCount(comment.getLikesCount() - 1);
        } else {
            comment.setDislikesCount(comment.getDislikesCount() - 1);
        }

        comment.setRating(comment.getLikesCount() - comment.getDislikesCount());

        commentLikeRepository.delete(existingLike);
        commentRepository.save(comment);
    }

    private CommentDto handleLike(Long userId, Long commentId, LikeType likeType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        CommentLike existingLike = commentLikeRepository.findByUserIdAndCommentId(userId, commentId).orElse(null);

        if (existingLike != null) {
            if (existingLike.getType() == likeType) {
                return CommentMapper.toCommentDto(comment);
            }

            if (existingLike.getType() == LikeType.LIKE) {
                comment.setLikesCount(comment.getLikesCount() - 1);
                comment.setDislikesCount(comment.getDislikesCount() + 1);
            } else {
                comment.setDislikesCount(comment.getDislikesCount() - 1);
                comment.setLikesCount(comment.getLikesCount() + 1);
            }

            existingLike.setType(likeType);
        } else {
            CommentLike newLike = CommentLike.builder()
                    .user(user)
                    .comment(comment)
                    .type(likeType)
                    .created(LocalDateTime.now())
                    .build();

            commentLikeRepository.save(newLike);

            if (likeType == LikeType.LIKE) {
                comment.setLikesCount(comment.getLikesCount() + 1);
            } else {
                comment.setDislikesCount(comment.getDislikesCount() + 1);
            }
        }

        comment.setRating(comment.getLikesCount() - comment.getDislikesCount());
        Comment updatedComment = commentRepository.save(comment);
        return CommentMapper.toCommentDto(updatedComment);
    }

    private CommentSort parseSortType(String sort) {
        if (sort == null) {
            return CommentSort.DATE;
        }
        try {
            return CommentSort.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CommentSort.DATE;
        }
    }
}
