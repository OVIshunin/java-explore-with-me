package ru.practicum.ewm.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
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
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    private RequestServiceImpl requestService;

    private User requester;
    private User initiator;
    private Event event;
    private ParticipationRequest request;

    @BeforeEach
    void setUp() {
        requester = User.builder()
                .id(2L)
                .name("Booker")
                .email("booker@example.com")
                .build();

        initiator = User.builder()
                .id(1L)
                .name("Owner")
                .email("owner@example.com")
                .build();

        event = Event.builder()
                .id(1L)
                .initiator(initiator)
                .state(EventState.PUBLISHED)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        request = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(requester)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();
    }

    // ==================== getUserRequests ====================

    @Test
    void getUserRequests_shouldReturnRequests() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(requestRepository.findByRequesterId(2L)).thenReturn(List.of(request));
        when(requestMapper.toDto(any(ParticipationRequest.class))).thenReturn(new ParticipationRequestDto());

        List<ParticipationRequestDto> result = requestService.getUserRequests(2L);

        assertThat(result).hasSize(1);
        verify(requestRepository).findByRequesterId(2L);
    }

    @Test
    void getUserRequests_shouldThrowNotFoundException_whenUserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getUserRequests(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id 999 not found");
    }

    // ==================== createRequest ====================

    @Test
    void createRequest_shouldCreateAndReturnRequest() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(1L, 2L)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(1L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestMapper.toEntity(any(User.class), any(Event.class))).thenReturn(request);
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(requestMapper.toDto(any(ParticipationRequest.class))).thenReturn(new ParticipationRequestDto());

        ParticipationRequestDto result = requestService.createRequest(2L, 1L);

        assertThat(result).isNotNull();
        verify(requestRepository).save(any(ParticipationRequest.class));
    }

    @Test
    void createRequest_shouldThrowNotFoundException_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.createRequest(999L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createRequest_shouldThrowNotFoundException_whenEventNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.createRequest(2L, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void createRequest_shouldThrowConflictException_whenInitiatorTriesToRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(initiator));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.createRequest(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Initiator cannot request participation in own event");
    }

    @Test
    void createRequest_shouldThrowConflictException_whenEventNotPublished() {
        event.setState(EventState.PENDING);

        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.createRequest(2L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Event is not published");
    }

    @Test
    void createRequest_shouldThrowConflictException_whenRequestAlreadyExists() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> requestService.createRequest(2L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Request already exists");
    }

    @Test
    void createRequest_shouldThrowConflictException_whenParticipantLimitReached() {
        event.setParticipantLimit(1);

        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(1L, 2L)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(1L, RequestStatus.CONFIRMED)).thenReturn(1L);

        assertThatThrownBy(() -> requestService.createRequest(2L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Participant limit reached");
    }

    @Test
    void createRequest_shouldAutoConfirm_whenRequestModerationDisabled() {
        event.setRequestModeration(false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(1L, 2L)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(1L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestMapper.toEntity(any(User.class), any(Event.class))).thenReturn(request);
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(requestMapper.toDto(any(ParticipationRequest.class))).thenReturn(new ParticipationRequestDto());

        requestService.createRequest(2L, 1L);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
        verify(requestRepository).save(any(ParticipationRequest.class));
    }

    // ==================== cancelRequest ====================

    @Test
    void cancelRequest_shouldCancelRequest() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(requestRepository.findByIdAndRequesterId(1L, 2L))
                .thenReturn(Optional.of(request));
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(requestMapper.toDto(any(ParticipationRequest.class))).thenReturn(new ParticipationRequestDto());

        ParticipationRequestDto result = requestService.cancelRequest(2L, 1L);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELED);
        verify(requestRepository).save(any(ParticipationRequest.class));
    }

    @Test
    void cancelRequest_shouldThrowNotFoundException_whenRequestNotFound() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(requestRepository.findByIdAndRequesterId(999L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.cancelRequest(2L, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Request not found");
    }

    // ==================== updateRequestStatus ====================

    @Test
    void updateRequestStatus_shouldConfirmRequest() {
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(List.of(1L))).thenReturn(List.of(request));
        when(requestRepository.countByEventIdAndStatus(1L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.saveAll(anyList())).thenReturn(List.of(request));
        when(requestMapper.toDto(any(ParticipationRequest.class))).thenReturn(new ParticipationRequestDto());

        EventRequestStatusUpdateResult result = requestService.updateRequestStatus(1L, 1L, updateRequest);

        assertThat(result.getConfirmedRequests()).hasSize(1);
        assertThat(result.getRejectedRequests()).isEmpty();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
    }

    @Test
    void updateRequestStatus_shouldRejectRequest() {
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.REJECTED)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(List.of(1L))).thenReturn(List.of(request));
        when(requestRepository.saveAll(anyList())).thenReturn(List.of(request));
        when(requestMapper.toDto(any(ParticipationRequest.class))).thenReturn(new ParticipationRequestDto());

        EventRequestStatusUpdateResult result = requestService.updateRequestStatus(1L, 1L, updateRequest);

        assertThat(result.getConfirmedRequests()).isEmpty();
        assertThat(result.getRejectedRequests()).hasSize(1);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void updateRequestStatus_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        assertThatThrownBy(() -> requestService.updateRequestStatus(1L, 999L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void updateRequestStatus_shouldThrowConflictException_whenNotInitiator() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        assertThatThrownBy(() -> requestService.updateRequestStatus(999L, 1L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Only initiator can update request status");
    }

    @Test
    void updateRequestStatus_shouldThrowConflictException_whenRequestNotPending() {
        request.setStatus(RequestStatus.CONFIRMED);

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(List.of(1L))).thenReturn(List.of(request));

        assertThatThrownBy(() -> requestService.updateRequestStatus(1L, 1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Request status must be PENDING");
    }

    @Test
    void updateRequestStatus_shouldRejectExtraRequests_whenLimitExceeded() {
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L, 2L))
                .status(RequestStatus.CONFIRMED)
                .build();

        event.setParticipantLimit(1);

        ParticipationRequest request2 = ParticipationRequest.builder()
                .id(2L)
                .event(event)
                .requester(requester)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(request, request2));
        when(requestRepository.countByEventIdAndStatus(1L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.saveAll(anyList())).thenReturn(List.of(request, request2));
        when(requestMapper.toDto(any(ParticipationRequest.class))).thenReturn(new ParticipationRequestDto());

        EventRequestStatusUpdateResult result = requestService.updateRequestStatus(1L, 1L, updateRequest);

        assertThat(result.getConfirmedRequests()).hasSize(1);
        assertThat(result.getRejectedRequests()).hasSize(1);
    }
}
