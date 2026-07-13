package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.LocationRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.integration.StatisticsIntegrationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final StatisticsIntegrationService statisticsService;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        log.info("Creating event for user id: {}, dto: {}", userId, dto);

        // Проверяем существование пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));

        // Проверяем категорию
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id " + dto.getCategory() + " not found"));

        // Проверяем дату события (не раньше чем через 2 часа)
        LocalDateTime now = LocalDateTime.now();
        if (dto.getEventDate().isBefore(now.plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }

        // Создаем локацию
        Location location = locationMapper.toEntity(dto.getLocation());
        location = locationRepository.save(location);

        // Создаем событие
        Event event = eventMapper.toEntity(dto, userId);
        event.setLocation(location);

        Event savedEvent = eventRepository.save(event);
        log.info("Event created with id: {}", savedEvent.getId());

        return eventMapper.toFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        log.info("Getting events for user id: {}, from: {}, size: {}", userId, from, size);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id " + userId + " not found");
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        Page<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        return events.getContent().stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.info("Getting event id: {} for user id: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found for user " + userId));

        return eventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Updating event id: {} for user id: {}, request: {}", eventId, userId, request);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found for user " + userId));

        // Проверяем статус события (можно изменить только PENDING или CANCELED)
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        // Обновляем поля
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id " + request.getCategory() + " not found"));
            event.setCategory(category);
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getEventDate() != null) {
            // Проверяем дату (не раньше чем через 2 часа)
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConflictException("Event date must be at least 2 hours from now");
            }
            event.setEventDate(request.getEventDate());
        }

        if (request.getLocation() != null) {
            Location location = locationMapper.toEntity(request.getLocation());
            location = locationRepository.save(location);
            event.setLocation(location);
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        // Обрабатываем изменение статуса
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Event id: {} updated", eventId);

        return eventMapper.toFullDto(updatedEvent);
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Integer from, Integer size) {
        log.info("Getting public events with text: {}, categories: {}, paid: {}, rangeStart: {}, rangeEnd: {}, " +
                        "onlyAvailable: {}, sort: {}, from: {}, size: {}", text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size);

        // Если диапазон дат не указан, ищем события после текущего момента
        if (rangeStart == null && rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        // Создаем сортировку
        Sort sortOrder;
        if (sort != null && sort.equalsIgnoreCase("VIEWS")) {
            // Для сортировки по просмотрам нужно получить все события и отсортировать в коде
            Pageable pageable = PageRequest.of(0, from + size); // Запрашиваем с запасом
            Page<Event> eventsPage = eventRepository.findEventsPublic(text, categories, paid, rangeStart, rangeEnd, pageable);
            List<Event> events = eventsPage.getContent();

            // Получаем просмотры для всех событий
            List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
            Map<Long, Long> viewsMap = statisticsService.getViews(eventIds);

            // Сортируем по просмотрам (по убыванию)
            events.sort((e1, e2) -> {
                Long v1 = viewsMap.getOrDefault(e1.getId(), 0L);
                Long v2 = viewsMap.getOrDefault(e2.getId(), 0L);
                return v2.compareTo(v1); // по убыванию
            });

            // Применяем пагинацию вручную
            events = events.stream()
                    .skip(from)
                    .limit(size)
                    .collect(Collectors.toList());

            return eventMapper.toShortDtoList(events);
        } else {
            // Сортировка по дате (по умолчанию)
            sortOrder = Sort.by("eventDate").ascending();
            Pageable pageable = PageRequest.of(from / size, size, sortOrder);
            Page<Event> events = eventRepository.findEventsPublic(text, categories, paid, rangeStart, rangeEnd, pageable);
            return eventMapper.toShortDtoList(events.getContent());
        }
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId) {
        log.info("Getting public event by id: {}", eventId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Integer from, Integer size) {
        log.info("Getting admin events with users: {}, states: {}, categories: {}, rangeStart: {}, rangeEnd: {}, " +
                "from: {}, size: {}", users, states, categories, rangeStart, rangeEnd, from, size);

        // Преобразуем строковые статусы в Enum
        List<EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(from / size, size);

        Page<Event> events = eventRepository.findEventsByAdmin(users, eventStates, categories,
                rangeStart, rangeEnd, pageable);

        return events.getContent().stream()
                .map(eventMapper::toFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Updating event by admin, eventId: {}, request: {}", eventId, request);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));

        // Обновляем поля
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id " + request.getCategory() + " not found"));
            event.setCategory(category);
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getEventDate() != null) {
            // Проверяем дату (не раньше чем за час от публикации)
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Event date must be at least 1 hour from now for publication");
            }
            event.setEventDate(request.getEventDate());
        }

        if (request.getLocation() != null) {
            Location location = locationMapper.toEntity(request.getLocation());
            location = locationRepository.save(location);
            event.setLocation(location);
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        // Обрабатываем изменение статуса
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT:
                    // Можно публиковать только PENDING
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    // Можно отклонить только неопубликованное
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already published");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Event id: {} updated by admin", eventId);

        return eventMapper.toFullDto(updatedEvent);
    }
}