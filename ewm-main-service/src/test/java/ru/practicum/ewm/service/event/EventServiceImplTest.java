package ru.practicum.ewm.service.event;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.exception.BadRequestException;
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
    private LocationRepository locationRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private LocationMapper locationMapper;
    @Mock
    private StatisticsIntegrationService statisticsService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private EventServiceImpl eventService;

    private User user;
    private Category category;
    private Location location;
    private Event event;
    private NewEventDto newEventDto;
    private EventFullDto eventFullDto;
    private LocationDto locationDto;

    @BeforeEach
    void setUp() {
        locationDto = LocationDto.builder()
                .lat(55.7558f)
                .lon(37.6173f)
                .build();

        newEventDto = NewEventDto.builder()
                .annotation("Test annotation for event")
                .category(1L)
                .description("Test description for event that is long enough")
                .eventDate(LocalDateTime.now().plusHours(3))
                .location(locationDto)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .title("Test Event")
                .build();

        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        location = Location.builder()
                .id(1L)
                .lat(55.7558f)
                .lon(37.6173f)
                .build();

        event = Event.builder()
                .id(1L)
                .annotation("Test annotation for event")
                .category(category)
                .description("Test description for event that is long enough")
                .eventDate(LocalDateTime.now().plusHours(3))
                .createdOn(LocalDateTime.now())
                .initiator(user)
                .location(location)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING)
                .title("Test Event")
                .build();

        eventFullDto = EventFullDto.builder()
                .id(1L)
                .annotation("Test annotation for event")
                .build();
    }

    // ==================== createEvent ====================

    @Test
    void createEvent_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(locationMapper.toEntity(locationDto)).thenReturn(location);
        when(locationRepository.save(any(Location.class))).thenReturn(location);
        when(eventMapper.toEntity(newEventDto, 1L)).thenReturn(event);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        EventFullDto result = eventService.createEvent(1L, newEventDto);

        assertThat(result).isNotNull();
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_userNotFound_throwsNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(1L, newEventDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id 1 not found");
    }

    @Test
    void createEvent_categoryNotFound_throwsNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(1L, newEventDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id 1 not found");
    }


    // ==================== getPublicEventById ====================

    @Test
    void getPublicEventById_success_savesHitToStats() {
        when(eventRepository.findByIdAndState(1L, EventState.PUBLISHED)).thenReturn(Optional.of(event));
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        // ДОБАВЛЯЕМ моки для request
        when(request.getRequestURI()).thenReturn("/events/1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        EventFullDto result = eventService.getPublicEventById(1L, request);

        assertThat(result).isNotNull();
        verify(statisticsService).saveHit(null, "/events/1", "127.0.0.1");
    }

    @Test
    void getPublicEventById_eventNotFound_throwsNotFoundException() {
        when(eventRepository.findByIdAndState(1L, EventState.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getPublicEventById(1L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id 1 not found");
    }

    @Test
    void getPublicEventById_notPublished_throwsNotFoundException() {
        event.setState(EventState.PENDING);
        when(eventRepository.findByIdAndState(1L, EventState.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getPublicEventById(1L, request))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== updateUserEvent ====================

    @Test
    void updateUserEvent_success() {
        UpdateEventUserRequest requestDto = UpdateEventUserRequest.builder()
                .title("Updated Title")
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        EventFullDto result = eventService.updateUserEvent(1L, 1L, requestDto);

        assertThat(result).isNotNull();
        verify(eventRepository).save(event);
    }

    @Test
    void updateUserEvent_eventNotFound_throwsNotFoundException() {
        UpdateEventUserRequest requestDto = UpdateEventUserRequest.builder().build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateUserEvent(1L, 1L, requestDto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateUserEvent_eventAlreadyPublished_throwsConflictException() {
        event.setState(EventState.PUBLISHED);
        UpdateEventUserRequest requestDto = UpdateEventUserRequest.builder()
                .title("Updated Title")
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateUserEvent(1L, 1L, requestDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only pending or canceled events can be changed");
    }

    @Test
    void updateUserEvent_dateTooSoon_throwsBadRequestException() {
        UpdateEventUserRequest requestDto = UpdateEventUserRequest.builder()
                .eventDate(LocalDateTime.now().plusMinutes(30))
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateUserEvent(1L, 1L, requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Event date must be at least 2 hours from now");
    }

    @Test
    void updateUserEvent_cancelReview_success() {
        UpdateEventUserRequest requestDto = UpdateEventUserRequest.builder()
                .stateAction(UpdateEventUserRequest.StateAction.CANCEL_REVIEW)
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        eventService.updateUserEvent(1L, 1L, requestDto);

        assertThat(event.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void updateUserEvent_sendToReview_success() {
        event.setState(EventState.CANCELED);
        UpdateEventUserRequest requestDto = UpdateEventUserRequest.builder()
                .stateAction(UpdateEventUserRequest.StateAction.SEND_TO_REVIEW)
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        eventService.updateUserEvent(1L, 1L, requestDto);

        assertThat(event.getState()).isEqualTo(EventState.PENDING);
    }

    // ==================== updateAdminEvent ====================

    @Test
    void updateAdminEvent_publish_success() {
        UpdateEventAdminRequest requestDto = UpdateEventAdminRequest.builder()
                .stateAction(UpdateEventAdminRequest.StateAction.PUBLISH_EVENT)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        eventService.updateAdminEvent(1L, requestDto);

        assertThat(event.getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(event.getPublishedOn()).isNotNull();
    }

    @Test
    void updateAdminEvent_publish_wrongState_throwsConflictException() {
        event.setState(EventState.PUBLISHED);
        UpdateEventAdminRequest requestDto = UpdateEventAdminRequest.builder()
                .stateAction(UpdateEventAdminRequest.StateAction.PUBLISH_EVENT)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateAdminEvent(1L, requestDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot publish the event because it's not in the right state");
    }

    @Test
    void updateAdminEvent_reject_success() {
        UpdateEventAdminRequest requestDto = UpdateEventAdminRequest.builder()
                .stateAction(UpdateEventAdminRequest.StateAction.REJECT_EVENT)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        eventService.updateAdminEvent(1L, requestDto);

        assertThat(event.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void updateAdminEvent_reject_published_throwsConflictException() {
        event.setState(EventState.PUBLISHED);
        UpdateEventAdminRequest requestDto = UpdateEventAdminRequest.builder()
                .stateAction(UpdateEventAdminRequest.StateAction.REJECT_EVENT)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateAdminEvent(1L, requestDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot reject the event because it's already published");
    }

    @Test
    void updateAdminEvent_dateTooSoon_throwsBadRequestException() {
        UpdateEventAdminRequest requestDto = UpdateEventAdminRequest.builder()
                .eventDate(LocalDateTime.now().plusMinutes(30))
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateAdminEvent(1L, requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Event date must be at least 1 hour from now for publication");
    }

    // ==================== getUserEvents ====================

    @Test
    void getUserEvents_success() {
        Page<Event> eventPage = new PageImpl<>(List.of(event));

        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByInitiatorId(eq(1L), any(Pageable.class))).thenReturn(eventPage);
        when(eventMapper.toShortDto(event)).thenReturn(EventShortDto.builder().id(1L).build());

        List<EventShortDto> result = eventService.getUserEvents(1L, 0, 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserEvents_userNotFound_throwsNotFoundException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> eventService.getUserEvents(1L, 0, 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id 1 not found");
    }

    // ==================== getUserEventById ====================

    @Test
    void getUserEventById_success() {
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        EventFullDto result = eventService.getUserEventById(1L, 1L);

        assertThat(result).isNotNull();
    }

    @Test
    void getUserEventById_notFound_throwsNotFoundException() {
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getUserEventById(1L, 1L))
                .isInstanceOf(NotFoundException.class);
    }
}