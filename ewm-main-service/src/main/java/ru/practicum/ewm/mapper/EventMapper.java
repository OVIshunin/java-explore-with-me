package ru.practicum.ewm.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.integration.StatisticsIntegrationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestRepository requestRepository;
    private final LocationMapper locationMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final StatisticsIntegrationService statisticsService;

    public Event toEntity(NewEventDto dto, Long userId) {
        if (dto == null) {
            return null;
        }

        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Location location = locationMapper.toEntity(dto.getLocation());

        return Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .createdOn(LocalDateTime.now())
                .initiator(initiator)
                .location(location)
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .state(EventState.PENDING)
                .title(dto.getTitle())
                .build();
    }

    public EventShortDto toShortDto(Event event) {
        if (event == null) {
            return null;
        }

        Long confirmedRequests = requestRepository.countConfirmedRequests(event.getId());
        Long views = statisticsService.getViews(event.getId());

        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .initiator(userMapper.toShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public EventFullDto toFullDto(Event event) {
        if (event == null) {
            return null;
        }

        Long confirmedRequests = requestRepository.countConfirmedRequests(event.getId());
        Long views = statisticsService.getViews(event.getId());

        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(userMapper.toShortDto(event.getInitiator()))
                .location(locationMapper.toDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        // Получаем все ID событий
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        // Получаем просмотры для всех событий одним запросом
        Map<Long, Long> viewsMap = statisticsService.getViews(eventIds);

        // Получаем количество подтвержденных запросов для всех событий
        Map<Long, Long> confirmedRequestsMap = events.stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        e -> requestRepository.countConfirmedRequests(e.getId())
                ));

        return events.stream()
                .map(event -> EventShortDto.builder()
                        .id(event.getId())
                        .annotation(event.getAnnotation())
                        .category(categoryMapper.toDto(event.getCategory()))
                        .confirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L))
                        .eventDate(event.getEventDate())
                        .initiator(userMapper.toShortDto(event.getInitiator()))
                        .paid(event.getPaid())
                        .title(event.getTitle())
                        .views(viewsMap.getOrDefault(event.getId(), 0L))
                        .build())
                .collect(Collectors.toList());
    }
}