package ru.practicum.ewm.category.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializeCategoryDto_shouldReturnValidJson() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .name("Концерты")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"name\":\"Концерты\"");
    }

    @Test
    void deserializeCategoryDto_shouldReturnValidObject() throws Exception {
        String json = "{\"id\": 1, \"name\": \"Концерты\"}";

        CategoryDto result = objectMapper.readValue(json, CategoryDto.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Концерты");
    }
}