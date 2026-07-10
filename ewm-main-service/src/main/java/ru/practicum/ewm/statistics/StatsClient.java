package ru.practicum.ewm.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.statsservice.client.StatsClientImpl;
import ru.practicum.statsservice.dto.EndpointHit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {

    private final StatsClientImpl statsClientImpl;

    @Value("${spring.application.name:ewm-main-service}")
    private String appName;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void saveHit(Long eventId) {
        try {
            EndpointHit hit = EndpointHit.builder()
                    .app(appName)
                    .uri("/events/" + eventId)
                    .ip("0.0.0.0")
                    .timestamp(LocalDateTime.now())
                    .build();
            statsClientImpl.saveHit(hit);
        } catch (Exception e) {
            log.warn("Failed to save hit for event {}: {}", eventId, e.getMessage());
        }
    }

    public Long getViews(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(10);
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            String uri = "/events/" + eventId;

            List<ru.practicum.statsservice.dto.ViewStats> stats =
                    statsClientImpl.getStats(start, end, List.of(uri), false);

            if (stats == null || stats.isEmpty()) {
                return 0L;
            }
            return stats.get(0).getHits();
        } catch (Exception e) {
            log.warn("Failed to get views for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    public Map<Long, Long> getViews(List<Long> eventIds) {
        try {
            if (eventIds == null || eventIds.isEmpty()) {
                return Map.of();
            }

            LocalDateTime start = LocalDateTime.now().minusYears(10);
            LocalDateTime end = LocalDateTime.now().plusDays(1);

            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            List<ru.practicum.statsservice.dto.ViewStats> stats =
                    statsClientImpl.getStats(start, end, uris, false);

            return stats.stream()
                    .collect(Collectors.toMap(
                            s -> {
                                String[] parts = s.getUri().split("/");
                                return Long.parseLong(parts[parts.length - 1]);
                            },
                            ru.practicum.statsservice.dto.ViewStats::getHits
                    ));
        } catch (Exception e) {
            log.warn("Failed to get views for events: {}", e.getMessage());
            return Map.of();
        }
    }
}