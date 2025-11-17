package ru.practicum.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.CommentAdminUpdateDto;
import ru.practicum.dto.CommentDto;
import ru.practicum.model.enums.CommentStatus;
import ru.practicum.service.CommentService;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getCommentsForModeration(@RequestParam(required = false) CommentStatus status,
                                                     @RequestParam(defaultValue = "0") Integer from,
                                                     @RequestParam(defaultValue = "10") Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return commentService.getCommentsForModeration(status, pageable);
    }

    @PatchMapping("/{commentId}")
    public CommentDto moderateComment(@PathVariable Long commentId,
                                      @Valid @RequestBody CommentAdminUpdateDto updateDto) {
        return commentService.moderateComment(commentId, updateDto);
    }
}