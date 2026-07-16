package ru.practicum.ewm.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.model.CommentStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommentDto {

    private Long id;
    private String text;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private CommentStatus status;
    private UserShortDto author;
    private Long eventId;
    private String eventTitle;
}