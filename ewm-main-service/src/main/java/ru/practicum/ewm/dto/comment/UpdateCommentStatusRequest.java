package ru.practicum.ewm.dto.comment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.CommentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentStatusRequest {

    @NotNull(message = "Status cannot be null")
    private CommentStatus status; // PUBLISHED или REJECTED
}