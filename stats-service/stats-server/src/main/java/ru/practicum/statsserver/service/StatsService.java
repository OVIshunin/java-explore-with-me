package ru.practicum.statsserver.service;

import ru.practicum.statsservice.dto.EndpointHit;
import ru.practicum.statsservice.dto.ViewStats;
import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    void saveHit(EndpointHit hit);

    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}