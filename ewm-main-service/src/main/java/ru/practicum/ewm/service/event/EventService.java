package ru.practicum.ewm.service.event;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.event.UpdateEventUserRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    // Private endpoints
    EventFullDto createEvent(Long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    // Public endpoints
    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, String sort, Integer from, Integer size);

    EventFullDto getPublicEventById(Long eventId);

    // Admin endpoints
    List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                      LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                      Integer from, Integer size);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);

    EventFullDto getPublicEventById(Long eventId, HttpServletRequest request);

}