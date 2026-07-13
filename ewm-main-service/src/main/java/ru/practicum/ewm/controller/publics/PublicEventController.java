package ru.practicum.ewm.controller.publics;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.service.event.EventService;
import ru.practicum.ewm.service.integration.StatisticsIntegrationService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;
    private final StatisticsIntegrationService statisticsService;

    @GetMapping
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        log.info("GET /events - Getting public events, text: {}, categories: {}, paid: {}, " +
                        "rangeStart: {}, rangeEnd: {}, onlyAvailable: {}, sort: {}, from: {}, size: {}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        // 1. Проверка text - если передан, должен быть не пустым
        if (text != null && text.trim().isEmpty()) {
            throw new BadRequestException("Text parameter cannot be empty");
        }

        // 2. Проверка categories - если передан список, все id должны быть > 0
        if (categories != null) {
            for (Long catId : categories) {
                if (catId == null || catId <= 0) {
                    throw new BadRequestException("Category id must be positive");
                }
            }
        }

        // 3. Проверка диапазона дат
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("Range end must be after range start");
        }

        // 4. Проверка from и size
        if (from < 0) {
            throw new BadRequestException("From must be 0 or positive");
        }
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than 0");
        }



        // Сохраняем статистику запроса
        statisticsService.saveHit(
                null, // будет использовано appName из конфига
                request.getRequestURI(),
                request.getRemoteAddr()
        );

        return eventService.getPublicEvents(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(
            @PathVariable Long id,
            HttpServletRequest request) {
        log.info("GET /events/{} - Getting public event by id", id);
        return eventService.getPublicEventById(id, request);
    }
}