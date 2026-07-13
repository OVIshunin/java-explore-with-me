package ru.practicum.ewm.service.integration;

import ru.practicum.statsservice.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StatisticsIntegrationService {

    void saveHit(String app, String uri, String ip);

    Map<Long, Long> getViews(List<Long> eventIds);

    Long getViews(Long eventId);

    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}