package ru.practicum.ewm.service.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Creating request for user {} to event {}", userId, eventId);

        // Проверяем пользователя
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));

        // Проверяем событие
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));

        // Проверяем, что событие опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        // Проверяем, что инициатор события не создает запрос
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Event initiator cannot create request to participate");
        }

        // Проверяем, что запрос уже существует
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Request already exists");
        }

        // Проверяем лимит участников
        long confirmedRequests = requestRepository.countConfirmedRequests(eventId);
        int participantLimit = event.getParticipantLimit();

        if (participantLimit > 0 && confirmedRequests >= participantLimit) {
            throw new ConflictException("Participant limit has been reached");
        }

        // Создаем запрос
        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .build();

        // Если пре-модерация отключена или лимит 0, сразу подтверждаем
        if (!event.getRequestModeration() || participantLimit == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Request created with id: {}, status: {}", savedRequest.getId(), savedRequest.getStatus());

        return requestMapper.toDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Getting requests for user {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id " + userId + " not found");
        }

        return requestRepository.findByRequesterId(userId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Canceling request {} for user {}", requestId, userId);

        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id " + requestId + " not found for user " + userId));

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        log.info("Request {} canceled", requestId);
        return requestMapper.toDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Getting requests for event {} by user {}", eventId, userId);

        // Проверяем, что пользователь - инициатор события
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found for user " + userId));

        return requestRepository.findByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.info("Updating request status for event {} by user {}, update: {}", eventId, userId, updateRequest);

        // Проверяем событие
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found for user " + userId));

        // Проверяем, что пре-модерация включена и есть лимит
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Request moderation is disabled or participant limit is 0");
        }

        // Проверяем, что событие не опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event is not published");
        }

        // Получаем запросы
        List<ParticipationRequest> requests = requestRepository.findByEventIdAndRequestIds(eventId,
                updateRequest.getRequestIds());

        if (requests.isEmpty()) {
            throw new NotFoundException("Requests not found");
        }

        // Проверяем, что все запросы в статусе PENDING
        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        // Проверяем лимит
        long currentConfirmed = requestRepository.countConfirmedRequests(eventId);
        int participantLimit = event.getParticipantLimit();

        if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
            // Проверяем, не превышен ли лимит
            if (currentConfirmed + updateRequest.getRequestIds().size() > participantLimit) {
                throw new ConflictException("Participant limit has been reached");
            }

            // Подтверждаем запросы
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(requestMapper.toDto(request));
            }

            // Если лимит достигнут, отклоняем все остальные PENDING запросы
            if (currentConfirmed + updateRequest.getRequestIds().size() == participantLimit) {
                List<ParticipationRequest> pendingRequests = requestRepository.findByEventId(eventId).stream()
                        .filter(r -> r.getStatus() == RequestStatus.PENDING)
                        .filter(r -> !updateRequest.getRequestIds().contains(r.getId()))
                        .collect(Collectors.toList());

                for (ParticipationRequest request : pendingRequests) {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(requestMapper.toDto(request));
                }
            }
        } else if (updateRequest.getStatus() == RequestStatus.REJECTED) {
            // Отклоняем запросы
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(requestMapper.toDto(request));
            }
        }

        requestRepository.saveAll(requests);
        log.info("Request statuses updated for event {}", eventId);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }
}