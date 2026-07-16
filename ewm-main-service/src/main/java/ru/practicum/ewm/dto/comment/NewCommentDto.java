package ru.practicum.ewm.dto.comment;


import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {

    @Size(min = 3, max = 7000, message = "Text must be between 3 and 7000 characters")
    private String text;
}