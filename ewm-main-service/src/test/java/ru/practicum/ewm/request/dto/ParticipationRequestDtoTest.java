package ru.practicum.ewm.request.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.request.model.RequestStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipationRequestDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void serializeParticipationRequestDto_shouldReturnValidJson() throws Exception {
        ParticipationRequestDto dto = ParticipationRequestDto.builder()
                .id(1L)
                .event(1L)
                .requester(2L)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.of(2026, 7, 10, 11, 30, 0))
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"event\":1");
        assertThat(json).contains("\"requester\":2");
        assertThat(json).contains("\"status\":\"PENDING\"");
    }

    @Test
    void deserializeParticipationRequestDto_shouldReturnValidObject() throws Exception {
        String json = """
                {
                    "id": 1,
                    "event": 1,
                    "requester": 2,
                    "status": "PENDING",
                    "created": "2026-07-10T11:30:00"
                }
                """;

        ParticipationRequestDto result = objectMapper.readValue(json, ParticipationRequestDto.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEvent()).isEqualTo(1L);
        assertThat(result.getRequester()).isEqualTo(2L);
        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
    }
}