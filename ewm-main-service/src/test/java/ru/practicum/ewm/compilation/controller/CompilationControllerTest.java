package ru.practicum.ewm.compilation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.test.BaseControllerTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompilationController.class)
class CompilationControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== ADMIN ====================

    @Test
    void createCompilation_shouldReturnCreated() throws Exception {
        NewCompilationDto input = NewCompilationDto.builder()
                .title("Летние концерты")
                .pinned(true)
                .events(java.util.List.of(1L, 2L))
                .build();

        CompilationDto output = CompilationDto.builder()
                .id(1L)
                .title("Летние концерты")
                .pinned(true)
                .build();

        when(compilationService.createCompilation(any(NewCompilationDto.class))).thenReturn(output);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Летние концерты"))
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    void createCompilation_shouldReturnBadRequest_whenTitleEmpty() throws Exception {
        NewCompilationDto input = NewCompilationDto.builder()
                .title("")
                .pinned(true)
                .build();

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCompilation_shouldReturnNoContent() throws Exception {
        doNothing().when(compilationService).deleteCompilation(1L);

        mockMvc.perform(delete("/admin/compilations/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCompilation_shouldReturnNotFound() throws Exception {
        doThrow(new NotFoundException("Compilation not found"))
                .when(compilationService).deleteCompilation(999L);

        mockMvc.perform(delete("/admin/compilations/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCompilation_shouldReturnUpdated() throws Exception {
        UpdateCompilationRequest input = UpdateCompilationRequest.builder()
                .title("Зимние концерты")
                .pinned(false)
                .build();

        CompilationDto output = CompilationDto.builder()
                .id(1L)
                .title("Зимние концерты")
                .pinned(false)
                .build();

        when(compilationService.updateCompilation(any(Long.class), any(UpdateCompilationRequest.class)))
                .thenReturn(output);

        mockMvc.perform(patch("/admin/compilations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Зимние концерты"))
                .andExpect(jsonPath("$.pinned").value(false));
    }

    @Test
    void updateCompilation_shouldReturnNotFound() throws Exception {
        UpdateCompilationRequest input = UpdateCompilationRequest.builder()
                .title("Зимние концерты")
                .build();

        when(compilationService.updateCompilation(any(Long.class), any(UpdateCompilationRequest.class)))
                .thenThrow(new NotFoundException("Compilation not found"));

        mockMvc.perform(patch("/admin/compilations/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    // ==================== PUBLIC ====================

    @Test
    void getCompilations_shouldReturnList() throws Exception {
        CompilationDto output = CompilationDto.builder()
                .id(1L)
                .title("Летние концерты")
                .pinned(true)
                .build();

        when(compilationService.getCompilations(null, 0, 10))
                .thenReturn(java.util.List.of(output));

        mockMvc.perform(get("/compilations")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Летние концерты"));
    }

    @Test
    void getCompilations_shouldFilterByPinned() throws Exception {
        CompilationDto output = CompilationDto.builder()
                .id(1L)
                .title("Закреплённая подборка")
                .pinned(true)
                .build();

        when(compilationService.getCompilations(true, 0, 10))
                .thenReturn(java.util.List.of(output));

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pinned").value(true));
    }

    @Test
    void getCompilations_shouldReturnEmptyList() throws Exception {
        when(compilationService.getCompilations(null, 0, 10))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/compilations")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getCompilationById_shouldReturnCompilation() throws Exception {
        CompilationDto output = CompilationDto.builder()
                .id(1L)
                .title("Летние концерты")
                .pinned(true)
                .build();

        when(compilationService.getCompilationById(1L)).thenReturn(output);

        mockMvc.perform(get("/compilations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Летние концерты"));
    }

    @Test
    void getCompilationById_shouldReturnNotFound() throws Exception {
        when(compilationService.getCompilationById(999L))
                .thenThrow(new NotFoundException("Compilation not found"));

        mockMvc.perform(get("/compilations/999"))
                .andExpect(status().isNotFound());
    }
}