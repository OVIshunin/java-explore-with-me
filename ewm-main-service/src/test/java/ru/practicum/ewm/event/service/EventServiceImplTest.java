package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.statistics.StatsClient;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private EventServiceImpl eventService;

    private User initiator;
    private Category category;
    private Event event;
    private NewEventDto newEventDto;
    private Location location;

    @BeforeEach
    void setUp() {
        initiator = User.builder()
                .id(1L)
                .name("John")
                .email("john@example.com")
                .build();

        category = Category.builder()
                .id(1L)
                .name("Концерты")
                .build();

        location = Location.builder()
                .lat(55.75)
                .lon(37.62)
                .build();

        LocalDateTime now = LocalDateTime.now().plusHours(3);

        event = Event.builder()
                .id(1L)
                .annotation("Краткое описание события")
                .category(category)
                .description("Полное описание события")
                .eventDate(now)
                .location(location)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .title("Заголовок события")
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .initiator(initiator)
                .build();

        newEventDto = NewEventDto.builder()
                .annotation("Краткое описание события")
                .category(1L)
                .description("Полное описание события")
                .eventDate(now)
                .location(location)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .title("Заголовок события")
                .build();
    }

    // ==================== createEvent ====================

    @Test
    void createEvent_shouldSaveAndReturnEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(initiator));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventMapper.toEntity(any(NewEventDto.class), any(User.class), any(Category.class)))
                .thenReturn(event);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toFullDto(any(Event.class))).thenReturn(EventFullDto.builder()
                .id(1L)
                .state(EventState.PENDING)
                .build());

        EventFullDto result = eventService.createEvent(1L, newEventDto);

        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(EventState.PENDING);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_shouldThrowNotFoundException_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(999L, newEventDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createEvent_shouldThrowNotFoundException_whenCategoryNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(initiator));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(1L, newEventDto.toBuilder().category(999L).build()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void createEvent_shouldThrowValidationException_whenEventDateInPast() {
        LocalDateTime pastDate = LocalDateTime.now().minusHours(1);
        NewEventDto pastEventDto = newEventDto.toBuilder()
                .eventDate(pastDate)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(initiator));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> eventService.createEvent(1L, pastEventDto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Event date must be at least 2 hours from now");
    }

    // ==================== getUserEvents ====================

    @Test
    void getUserEvents_shouldReturnListOfEvents() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByInitiatorId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(eventMapper.toShortDto(any(Event.class))).thenReturn(EventShortDto.builder()
                .id(1L)
                .title("Заголовок события")
                .build());

        var result = eventService.getUserEvents(1L, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getUserEvents_shouldThrowNotFoundException_whenUserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> eventService.getUserEvents(999L, 0, 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id 999 not found");
    }

    // ==================== updateUserEvent ====================

    @Test
    void updateUserEvent_shouldUpdateEvent() {
        UpdateEventUserRequest updateRequest = UpdateEventUserRequest.builder()
                .title("Новый заголовок")
                .build();

        Event updatedEvent = Event.builder()
                .id(1L)
                .title("Новый заголовок")
                .state(EventState.PENDING)
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(updatedEvent);
        when(eventMapper.toFullDto(any(Event.class))).thenReturn(EventFullDto.builder()
                .id(1L)
                .title("Новый заголовок")
                .build());

        EventFullDto result = eventService.updateUserEvent(1L, 1L, updateRequest);

        assertThat(result.getTitle()).isEqualTo("Новый заголовок");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void updateUserEvent_shouldThrowConflictException_whenEventAlreadyPublished() {
        event.setState(EventState.PUBLISHED);

        when(eventRepository.findByIdAndInitiatorId(1L, 1L))
                .thenReturn(Optional.of(event));

        UpdateEventUserRequest updateRequest = UpdateEventUserRequest.builder()
                .title("Новый заголовок")
                .build();

        assertThatThrownBy(() -> eventService.updateUserEvent(1L, 1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only pending or canceled events can be changed");
    }

    @Test
    void updateUserEvent_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventRepository.findByIdAndInitiatorId(999L, 1L))
                .thenReturn(Optional.empty());

        UpdateEventUserRequest updateRequest = UpdateEventUserRequest.builder()
                .title("Новый заголовок")
                .build();

        assertThatThrownBy(() -> eventService.updateUserEvent(1L, 999L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    // ==================== getPublicEvents ====================

    @Test
    void getPublicEvents_shouldReturnEvents_whenNoFilters() {
        when(eventRepository.findPublishedEventsWithFilters(
                any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(eventMapper.toShortDto(any(Event.class))).thenReturn(EventShortDto.builder()
                .id(1L)
                .title("Заголовок события")
                .build());

        var result = eventService.getPublicEvents(null, null, null, null, null, null, "EVENT_DATE", 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== getPublicEventById ====================

    @Test
    void getPublicEventById_shouldReturnEvent_whenPublished() {
        event.setState(EventState.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(statsClient.getViews(1L)).thenReturn(100L);
        when(requestRepository.countByEventIdAndStatus(eq(1L), any()))
                .thenReturn(5L);
        when(eventMapper.toFullDto(any(Event.class))).thenReturn(EventFullDto.builder()
                .id(1L)
                .title("Заголовок")
                .build());

        EventFullDto result = eventService.getPublicEventById(1L);

        assertThat(result).isNotNull();
        verify(statsClient).saveHit(1L);
    }

    @Test
    void getPublicEventById_shouldThrowNotFoundException_whenEventNotPublished() {
        event.setState(EventState.PENDING);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.getPublicEventById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event not published");
    }
}
