package ru.practicum.ewm.service.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.AdminCommentDto;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.util.OffsetPageRequest;


import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        log.info("Creating comment: userId={}, eventId={}", userId, eventId);

        User author = getUser(userId);
        Event event = getPublishedEvent(eventId);

        Comment comment = commentMapper.toEntity(dto.getText(), event, author);
        comment = commentRepository.save(comment);

        log.info("Comment created with id: {}", comment.getId());
        return commentMapper.toDto(comment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto) {
        log.info("Updating comment: userId={}, commentId={}", userId, commentId);

        Comment comment = getComment(commentId);

        // Проверяем, что пользователь — автор
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("User is not the author of this comment");
        }

        // Нельзя редактировать отклонённый комментарий
        if (comment.getStatus() == CommentStatus.REJECTED) {
            throw new ConflictException("Cannot edit rejected comment");
        }

        comment.setText(dto.getText());
        // Статус не меняется, updatedOn обновится автоматически через @UpdateTimestamp

        log.info("Comment updated: id={}", commentId);
        return commentMapper.toDto(comment);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        log.info("User deleting comment: userId={}, commentId={}", userId, commentId);

        Comment comment = getComment(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("User is not the author of this comment");
        }

        // Мягкое удаление
        comment.setStatus(CommentStatus.DELETED);
        log.info("Comment marked as DELETED: id={}", commentId);
    }

    @Override
    public List<CommentDto> getPublishedComments(Long eventId, int from, int size) {
        log.info("Getting published comments for event: {}", eventId);

        Pageable pageable = new OffsetPageRequest(from, size);
        List<Comment> comments = commentRepository.findByEventIdAndStatus(
                eventId, CommentStatus.PUBLISHED, pageable);

        return comments.stream()
                .map(commentMapper::toDto)
                .toList();
    }

    @Override
    public List<AdminCommentDto> getCommentsByStatus(CommentStatus status, int from, int size) {
        log.info("Getting comments by status: {}", status);

        Pageable pageable = new OffsetPageRequest(from, size);
        List<Comment> comments = commentRepository.findByStatus(status, pageable);

        return comments.stream()
                .map(commentMapper::toAdminDto)
                .toList();
    }

    @Override
    @Transactional
    public AdminCommentDto moderateComment(Long commentId, CommentStatus newStatus) {
        log.info("Moderating comment: id={}, newStatus={}", commentId, newStatus);

        Comment comment = getComment(commentId);

        // Админ может только PUBLISH или REJECT
        if (newStatus != CommentStatus.PUBLISHED && newStatus != CommentStatus.REJECTED) {
            throw new IllegalArgumentException("Admin can only PUBLISH or REJECT comments");
        }

        // Нельзя менять статус у уже удалённого комментария
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new ConflictException("Cannot moderate deleted comment");
        }

        comment.setStatus(newStatus);
        log.info("Comment status changed to: {}", newStatus);

        return commentMapper.toAdminDto(comment);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Admin deleting comment: {}", commentId);

        Comment comment = getComment(commentId);
        comment.setStatus(CommentStatus.DELETED);
        log.info("Comment marked as DELETED by admin: id={}", commentId);
    }

    // === Приватные методы ===

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
    }

    private Event getPublishedEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment on unpublished event");
        }

        return event;
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findByIdWithDetails(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
    }
}