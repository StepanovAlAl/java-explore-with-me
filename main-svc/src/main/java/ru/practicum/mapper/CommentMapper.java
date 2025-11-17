package ru.practicum.mapper;

import ru.practicum.dto.CommentDto;
import ru.practicum.model.Comment;

public class CommentMapper {
    public static CommentDto toCommentDto(Comment comment) {
        if (comment == null) {
            return null;
        }

        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setAuthor(UserMapper.toUserShortDto(comment.getAuthor()));
        dto.setEventId(comment.getEvent().getId());
        dto.setStatus(comment.getStatus().name());
        dto.setCreated(comment.getCreated());
        dto.setUpdated(comment.getUpdated());
        dto.setLikesCount(comment.getLikesCount());
        dto.setDislikesCount(comment.getDislikesCount());
        dto.setRating(comment.getRating());
        dto.setPinned(comment.getPinned());

        if (comment.getParentComment() != null) {
            dto.setParentCommentId(comment.getParentComment().getId());
        }

        return dto;
    }
}