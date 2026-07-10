package ru.practicum.ewm.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.test.BaseControllerTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;  // <- Здесь используем @Autowired

    @Autowired
    private ObjectMapper objectMapper;  // <- Здесь используем @Autowired

    @Test
    void createCategory_shouldReturnCreated() throws Exception {
        CategoryDto input = CategoryDto.builder()
                .name("Концерты")
                .build();

        CategoryDto output = CategoryDto.builder()
                .id(1L)
                .name("Концерты")
                .build();

        when(categoryService.createCategory(any(CategoryDto.class))).thenReturn(output);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Концерты"));
    }

    @Test
    void getCategoryById_shouldReturnCategory() throws Exception {
        CategoryDto output = CategoryDto.builder()
                .id(1L)
                .name("Концерты")
                .build();

        when(categoryService.getCategoryById(1L)).thenReturn(output);

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Концерты"));
    }

    @Test
    void getCategoryById_shouldReturnNotFound() throws Exception {
        when(categoryService.getCategoryById(999L))
                .thenThrow(new NotFoundException("Category not found"));

        mockMvc.perform(get("/categories/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCategory_shouldReturnUpdated() throws Exception {
        CategoryDto input = CategoryDto.builder()
                .name("Выставки")
                .build();

        CategoryDto output = CategoryDto.builder()
                .id(1L)
                .name("Выставки")
                .build();

        when(categoryService.updateCategory(any(Long.class), any(CategoryDto.class)))
                .thenReturn(output);

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Выставки"));
    }

    @Test
    void deleteCategory_shouldReturnNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getCategories_shouldReturnList() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .name("Концерты")
                .build();

        when(categoryService.getCategories(0, 10)).thenReturn(java.util.List.of(dto));

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Концерты"));
    }
}
