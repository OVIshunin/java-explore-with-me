package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
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
public class UpdateEventAdminRequest {

    @Size(min = 20, max = 2000, message = "Annotation size must be between 20 and 2000")
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000, message = "Description size must be between 20 and 7000")
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @Valid
    private LocationDto location;

    private Boolean paid;

    @Valid
    @jakarta.validation.constraints.PositiveOrZero(message = "Participant limit must be 0 or positive")
    private Integer participantLimit;

    private Boolean requestModeration;

    private StateAction stateAction;

    @Size(min = 3, max = 120, message = "Title size must be between 3 and 120")
    private String title;

    public enum StateAction {
        PUBLISH_EVENT,
        REJECT_EVENT
    }
}