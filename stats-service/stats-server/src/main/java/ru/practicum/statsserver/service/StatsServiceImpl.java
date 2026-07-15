package ru.practicum.statsserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.statsservice.dto.EndpointHit;
import ru.practicum.statsservice.dto.ViewStats;
import ru.practicum.statsserver.model.EndpointHitEntity;
import ru.practicum.statsserver.mapper.StatsMapper;
import ru.practicum.statsserver.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final StatsMapper statsMapper;

    @Override
    @Transactional
    public void saveHit(EndpointHit hit) {
        log.info("Saving hit: app={}, uri={}, ip={}, timestamp={}",
                hit.getApp(), hit.getUri(), hit.getIp(), hit.getTimestamp());

        EndpointHitEntity entity = statsMapper.toEntity(hit);
        statsRepository.save(entity);

        statsRepository.flush();

        log.debug("Hit saved with id: {}", entity.getId());
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Getting stats from {} to {}, uris={}, unique={}", start, end, uris, unique);

        if (Boolean.TRUE.equals(unique)) {
            return statsRepository.findUniqueStats(start, end, uris);
        }
        return statsRepository.findStats(start, end, uris);
    }
}
