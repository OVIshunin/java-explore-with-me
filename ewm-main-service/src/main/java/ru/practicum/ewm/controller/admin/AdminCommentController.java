package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.AdminCommentDto;
import ru.practicum.ewm.dto.comment.UpdateCommentStatusRequest;
import ru.practicum.ewm.model.CommentStatus;
import ru.practicum.ewm.service.comment.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<AdminCommentDto> getCommentsByStatus(@RequestParam(required = false) CommentStatus status,
                                                     @RequestParam(defaultValue = "0") int from,
                                                     @RequestParam(defaultValue = "10") int size) {
        if (status == null) {
            status = CommentStatus.PENDING;
        }
        return commentService.getCommentsByStatus(status, from, size);
    }

    @PatchMapping("/{commentId}")
    public AdminCommentDto moderateComment(@PathVariable Long commentId,
                                           @Valid @RequestBody UpdateCommentStatusRequest request) {
        return commentService.moderateComment(commentId, request.getStatus());
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteCommentByAdmin(commentId);
    }
}