package ru.practicum.ewm.request.service;

import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;

import java.util.List;

public interface RequestService {

    // Получение заявок текущего пользователя
    List<ParticipationRequestDto> getUserRequests(Long userId);

    // Создание заявки на участие
    ParticipationRequestDto createRequest(Long userId, Long eventId);

    // Отмена заявки
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    // Получение заявок на событие (для инициатора)
    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    // Изменение статуса заявок (подтверждение/отклонение)
    EventRequestStatusUpdateResult updateRequestStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest updateRequest);
}