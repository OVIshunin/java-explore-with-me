package ru.practicum.statsserver.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.statsservice.dto.EndpointHit;
import ru.practicum.statsserver.model.EndpointHitEntity;

@Component
public class StatsMapper {

    public EndpointHitEntity toEntity(EndpointHit dto) {
        if (dto == null) {
            return null;
        }

        return EndpointHitEntity.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }

    public EndpointHit toDto(EndpointHitEntity entity) {
        if (entity == null) {
            return null;
        }

        return EndpointHit.builder()
                .id(entity.getId())
                .app(entity.getApp())
                .uri(entity.getUri())
                .ip(entity.getIp())
                .timestamp(entity.getTimestamp())
                .build();
    }
}