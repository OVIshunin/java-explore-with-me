package ru.practicum.ewm.compilation.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.event.dto.EventShortDto;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CompilationDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializeCompilationDto_shouldReturnValidJson() throws Exception {
        CompilationDto dto = CompilationDto.builder()
                .id(1L)
                .title("Летние концерты")
                .pinned(true)
                .events(List.of(
                        EventShortDto.builder().id(1L).title("Событие 1").build(),
                        EventShortDto.builder().id(2L).title("Событие 2").build()
                ))
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"title\":\"Летние концерты\"");
        assertThat(json).contains("\"pinned\":true");
        assertThat(json).contains("\"events\"");
    }

    @Test
    void deserializeCompilationDto_shouldReturnValidObject() throws Exception {
        String json = "{\"id\": 1, \"title\": \"Летние концерты\", \"pinned\": true, \"events\": []}";

        CompilationDto result = objectMapper.readValue(json, CompilationDto.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Летние концерты");
        assertThat(result.getPinned()).isTrue();
    }
}