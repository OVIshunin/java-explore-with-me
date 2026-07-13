package ru.practicum.ewm.service.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private ParticipationRequestRepository requestRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    private RequestServiceImpl requestService;

    private User user;
    private User anotherUser;
    private Event event;
    private ParticipationRequest request;
    private ParticipationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("User").build();
        anotherUser = User.builder().id(2L).name("Another User").build();

        event = Event.builder()
                .id(1L)
                .title("Test Event")
                .state(EventState.PUBLISHED)
                .initiator(anotherUser)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        request = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();

        requestDto = ParticipationRequestDto.builder()
                .id(1L)
                .event(1L)
                .requester(1L)
                .status(RequestStatus.PENDING)
                .build();
    }

    // ==================== createRequest ====================

    @Test
    void createRequest_success_pending() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(requestRepository.countConfirmedRequests(1L)).thenReturn(0L);
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestService.createRequest(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
    }


    @Test
    void createRequest_userNotFound_throwsNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.createRequest(1L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id 1 not found");
    }

    @Test
    void createRequest_eventNotFound_throwsNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.createRequest(1L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id 1 not found");
    }

    @Test
    void createRequest_eventNotPublished_throwsConflictException() {
        event.setState(EventState.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.createRequest(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot participate in unpublished event");
    }

    @Test
    void createRequest_initiator_throwsConflictException() {
        event.setInitiator(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.createRequest(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Event initiator cannot create request to participate");
    }

    @Test
    void createRequest_duplicate_throwsConflictException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> requestService.createRequest(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Request already exists");
    }

    @Test
    void createRequest_limitReached_throwsConflictException() {
        event.setParticipantLimit(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(requestRepository.countConfirmedRequests(1L)).thenReturn(1L);

        assertThatThrownBy(() -> requestService.createRequest(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Participant limit has been reached");
    }

    // ==================== getUserRequests ====================

    @Test
    void getUserRequests_success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findByRequesterId(1L)).thenReturn(List.of(request));
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        List<ParticipationRequestDto> result = requestService.getUserRequests(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserRequests_userNotFound_throwsNotFoundException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getUserRequests(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id 1 not found");
    }

    // ==================== cancelRequest ====================

    @Test
    void cancelRequest_success() {
        when(requestRepository.findByIdAndRequesterId(1L, 1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestService.cancelRequest(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELED);
    }

    @Test
    void cancelRequest_notFound_throwsNotFoundException() {
        when(requestRepository.findByIdAndRequesterId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.cancelRequest(1L, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== getEventRequests ====================

    @Test
    void getEventRequests_success() {
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventId(1L)).thenReturn(List.of(request));
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        List<ParticipationRequestDto> result = requestService.getEventRequests(1L, 1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getEventRequests_notInitiator_throwsNotFoundException() {
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.getEventRequests(1L, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== updateRequestStatus ====================

    @Test
    void updateRequestStatus_confirm_success() {
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequestIds(1L, List.of(1L))).thenReturn(List.of(request));
        when(requestRepository.countConfirmedRequests(1L)).thenReturn(0L);
        when(requestRepository.saveAll(any())).thenReturn(List.of(request));
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        EventRequestStatusUpdateResult result = requestService.updateRequestStatus(1L, 1L, updateRequest);

        assertThat(result.getConfirmedRequests()).hasSize(1);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
    }

    @Test
    void updateRequestStatus_moderationDisabled_throwsConflictException() {
        event.setRequestModeration(false);
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.updateRequestStatus(1L, 1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Request moderation is disabled or participant limit is 0");
    }

    @Test
    void updateRequestStatus_limitZero_throwsConflictException() {
        event.setParticipantLimit(0);
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.updateRequestStatus(1L, 1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Request moderation is disabled or participant limit is 0");
    }

    @Test
    void updateRequestStatus_requestNotPending_throwsConflictException() {
        request.setStatus(RequestStatus.CONFIRMED);
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.REJECTED)
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequestIds(1L, List.of(1L))).thenReturn(List.of(request));

        assertThatThrownBy(() -> requestService.updateRequestStatus(1L, 1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Request must have status PENDING");
    }

    @Test
    void updateRequestStatus_limitExceeded_throwsConflictException() {
        event.setParticipantLimit(1);
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L, 2L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequestIds(1L, List.of(1L, 2L))).thenReturn(List.of(request));
        when(requestRepository.countConfirmedRequests(1L)).thenReturn(0L);

        assertThatThrownBy(() -> requestService.updateRequestStatus(1L, 1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Participant limit has been reached");
    }

    @Test
    void updateRequestStatus_reject_success() {
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.REJECTED)
                .build();

        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequestIds(1L, List.of(1L))).thenReturn(List.of(request));
        when(requestRepository.saveAll(any())).thenReturn(List.of(request));
        when(requestMapper.toDto(request)).thenReturn(requestDto);

        EventRequestStatusUpdateResult result = requestService.updateRequestStatus(1L, 1L, updateRequest);

        assertThat(result.getRejectedRequests()).hasSize(1);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }
}