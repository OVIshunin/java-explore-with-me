package ru.practicum.statsserver.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.statsservice.dto.EndpointHit;
import ru.practicum.statsserver.model.EndpointHitEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StatsMapperTest {

    private final StatsMapper mapper = new StatsMapper();

    @Test
    void toEntity_shouldConvertDtoToEntity() {
        EndpointHit dto = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2026, 7, 2, 20, 10, 0))
                .build();

        EndpointHitEntity entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getApp()).isEqualTo("ewm-main-service");
        assertThat(entity.getUri()).isEqualTo("/events/1");
        assertThat(entity.getIp()).isEqualTo("192.168.0.1");
        assertThat(entity.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 7, 2, 20, 10, 0));
    }

    @Test
    void toEntity_shouldReturnNull_whenDtoIsNull() {
        EndpointHitEntity entity = mapper.toEntity(null);

        assertThat(entity).isNull();
    }

    @Test
    void toDto_shouldConvertEntityToDto() {
        EndpointHitEntity entity = EndpointHitEntity.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2026, 7, 2, 20, 10, 0))
                .build();

        EndpointHit dto = mapper.toDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getApp()).isEqualTo("ewm-main-service");
        assertThat(dto.getUri()).isEqualTo("/events/1");
        assertThat(dto.getIp()).isEqualTo("192.168.0.1");
        assertThat(dto.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 7, 2, 20, 10, 0));
    }

    @Test
    void toDto_shouldReturnNull_whenEntityIsNull() {
        EndpointHit dto = mapper.toDto(null);

        assertThat(dto).isNull();
    }
}