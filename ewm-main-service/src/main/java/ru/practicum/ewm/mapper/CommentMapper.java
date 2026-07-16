package ru.practicum.ewm.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.comment.AdminCommentDto;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;

@Component
public class CommentMapper {

    public Comment toEntity(String text, Event event, User author) {
        return Comment.builder()
                .text(text)
                .event(event)
                .author(author)
                .status(CommentStatus.PENDING)
                .build();
    }

    public CommentDto toDto(Comment comment) {
        if (comment == null) {
            return null;
        }

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .author(UserShortDto.builder()
                        .id(comment.getAuthor().getId())
                        .name(comment.getAuthor().getName())
                        .build())
                .build();
    }

    public AdminCommentDto toAdminDto(Comment comment) {
        if (comment == null) {
            return null;
        }

        return AdminCommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .status(comment.getStatus())
                .author(UserShortDto.builder()
                        .id(comment.getAuthor().getId())
                        .name(comment.getAuthor().getName())
                        .build())
                .eventId(comment.getEvent().getId())
                .eventTitle(comment.getEvent().getTitle())
                .build();
    }
}