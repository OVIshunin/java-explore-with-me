package ru.practicum.statsserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.statsserver.exception.BadRequestException;
import ru.practicum.statsservice.dto.EndpointHit;
import ru.practicum.statsservice.dto.ViewStats;
import ru.practicum.statsserver.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@Valid @RequestBody EndpointHit hit) {
        log.info("POST /hit - Saving hit: {}", hit);
        statsService.saveHit(hit);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {
        log.info("GET /stats - start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        // Проверка обязательности параметров
        if (start == null) {
            throw new BadRequestException("Start date is required");
        }
        if (end == null) {
            throw new BadRequestException("End date is required");
        }

        // Проверка, что start раньше end
        if (start.isAfter(end)) {
            throw new BadRequestException("Start date must be before end date");
        }

        // Если uris — пустой список, превращаем в null
        if (uris != null && uris.isEmpty()) {
            uris = null;
        }

        return statsService.getStats(start, end, uris, unique);
    }
}