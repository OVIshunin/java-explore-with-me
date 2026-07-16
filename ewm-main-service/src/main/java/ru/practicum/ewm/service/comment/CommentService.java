package ru.practicum.ewm.service.comment;

import ru.practicum.ewm.dto.comment.AdminCommentDto;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.model.CommentStatus;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, NewCommentDto dto);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto);

    void deleteCommentByUser(Long userId, Long commentId);

    List<CommentDto> getPublishedComments(Long eventId, int from, int size);

    List<AdminCommentDto> getCommentsByStatus(CommentStatus status, int from, int size);

    AdminCommentDto moderateComment(Long commentId, CommentStatus newStatus);

    void deleteCommentByAdmin(Long commentId);
}