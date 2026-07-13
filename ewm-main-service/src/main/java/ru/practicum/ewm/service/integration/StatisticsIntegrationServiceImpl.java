package ru.practicum.ewm.service.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.statsservice.client.StatsClient;
import ru.practicum.statsservice.dto.EndpointHit;
import ru.practicum.statsservice.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsIntegrationServiceImpl implements StatisticsIntegrationService {

    private final StatsClient statsClient;

    @Value("${spring.application.name:ewm-main-service}")
    private String appName;

    @Override
    public void saveHit(String app, String uri, String ip) {
        log.debug("Saving hit: app={}, uri={}, ip={}", app, uri, ip);

        EndpointHit hit = EndpointHit.builder()
                .app(app != null ? app : appName)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.saveHit(hit);
    }

    @Override
    public Map<Long, Long> getViews(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        log.debug("Getting views for events: {}", eventIds);

        // Формируем URI для каждого события
        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        // Запрашиваем статистику за все время (с начала 2020 года)
        LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.now();

        List<ViewStats> stats = statsClient.getStats(start, end, uris, false);

        // Преобразуем в Map<eventId, views>
        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> extractEventId(stat.getUri()),
                        ViewStats::getHits
                ));
    }

    @Override
    public Long getViews(Long eventId) {
        Map<Long, Long> views = getViews(List.of(eventId));
        return views.getOrDefault(eventId, 0L);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.debug("Getting stats: start={}, end={}, uris={}, unique={}", start, end, uris, unique);
        return statsClient.getStats(start, end, uris, unique);
    }

    private Long extractEventId(String uri) {
        // Из URI вида "/events/123" извлекаем 123
        try {
            String[] parts = uri.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("Failed to extract event id from uri: {}", uri);
            return null;
        }
    }
}