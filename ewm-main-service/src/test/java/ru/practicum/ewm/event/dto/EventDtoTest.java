package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.user.dto.UserShortDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void serializeEventFullDto_shouldReturnValidJson() throws Exception {
        EventFullDto dto = EventFullDto.builder()
                .id(1L)
                .annotation("Аннотация события")
                .description("Описание события")
                .title("Заголовок")
                .eventDate(LocalDateTime.of(2026, 12, 31, 15, 10, 5))
                .location(Location.builder().lat(55.75).lon(37.62).build())
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING)
                .category(CategoryDto.builder().id(1L).name("Концерты").build())
                .initiator(UserShortDto.builder().id(1L).name("John Doe").build())
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"title\":\"Заголовок\"");
        assertThat(json).contains("\"state\":\"PENDING\"");
    }

    @Test
    void deserializeEventFullDto_shouldReturnValidObject() throws Exception {
        String json = """
                {
                    "id": 1,
                    "annotation": "Аннотация события",
                    "description": "Описание события",
                    "title": "Заголовок",
                    "eventDate": "2026-12-31T15:10:05",
                    "location": {"lat": 55.75, "lon": 37.62},
                    "paid": true,
                    "participantLimit": 10,
                    "requestModeration": true,
                    "state": "PENDING"
                }
                """;

        EventFullDto result = objectMapper.readValue(json, EventFullDto.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAnnotation()).isEqualTo("Аннотация события");
        assertThat(result.getState()).isEqualTo(EventState.PENDING);
    }
}