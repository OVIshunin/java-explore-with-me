package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.location.LocationDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {

    @NotBlank(message = "annotation cannot be blank")
    @Size(min = 20, max = 2000, message = "annotation size must be between 20 and 2000")
    private String annotation;

    @NotNull(message = "category cannot be null")
    private Long category;

    @NotBlank(message = "description cannot be blank")
    @Size(min = 20, max = 7000, message = "description size must be between 20 and 7000")
    private String description;

    @NotNull(message = "event date cannot be null")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull(message = "location cannot be null")
    @Valid
    private LocationDto location;

    private Boolean paid = false;

    @PositiveOrZero(message = "participant limit must be 0 or positive")
    private Integer participantLimit = 0;

    private Boolean requestModeration = true;

    @NotBlank(message = "title cannot be blank")
    @Size(min = 3, max = 120, message = "title size must be between 3 and 120")
    private String title;
}