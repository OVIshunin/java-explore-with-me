package ru.practicum.ewm.controller.publics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.service.compilation.CompilationService;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    private CompilationDto compilationDto;

    @BeforeEach
    void setUp() {
        compilationDto = CompilationDto.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of())
                .build();
    }

    @Test
    void getCompilations_success() throws Exception {
        when(compilationService.getCompilations(any(), anyInt(), anyInt()))
                .thenReturn(List.of(compilationDto));

        mockMvc.perform(get("/compilations")
                        .param("from", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Compilation"));
    }

    @Test
    void getCompilations_withPinnedFilter_success() throws Exception {
        when(compilationService.getCompilations(eq(true), anyInt(), anyInt()))
                .thenReturn(List.of(compilationDto));

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getCompilationById_success() throws Exception {
        when(compilationService.getCompilationById(1L))
                .thenReturn(compilationDto);

        mockMvc.perform(get("/compilations/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Compilation"));
    }

    @Test
    void getCompilationById_notFound_throwsNotFound() throws Exception {
        when(compilationService.getCompilationById(999L))
                .thenThrow(new NotFoundException("Compilation with id 999 not found"));

        mockMvc.perform(get("/compilations/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}