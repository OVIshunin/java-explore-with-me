package ru.practicum.statsservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsRequestDto {

    @NotNull(message = "Start time cannot be null")
    private LocalDateTime start;

    @NotNull(message = "End time cannot be null")
    private LocalDateTime end;

    private List<String> uris;

    private Boolean unique;
}