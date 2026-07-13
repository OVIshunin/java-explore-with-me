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
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.service.event.EventService;
import ru.practicum.ewm.service.integration.StatisticsIntegrationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private StatisticsIntegrationService statisticsService;

    private EventShortDto eventShortDto;
    private EventFullDto eventFullDto;

    @BeforeEach
    void setUp() {
        eventShortDto = EventShortDto.builder()
                .id(1L)
                .annotation("Test Annotation")
                .title("Test Event")
                .paid(true)
                .eventDate(LocalDateTime.now().plusHours(3))
                .views(0L)
                .confirmedRequests(0L)
                .build();

        eventFullDto = EventFullDto.builder()
                .id(1L)
                .annotation("Test Annotation")
                .title("Test Event")
                .paid(true)
                .eventDate(LocalDateTime.now().plusHours(3))
                .views(0L)
                .confirmedRequests(0L)
                .build();
    }

    @Test
    void getEvents_success() throws Exception {
        when(eventService.getPublicEvents(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(eventShortDto));

        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Event"));
    }

    @Test
    void getEvents_withFilters_success() throws Exception {
        when(eventService.getPublicEvents(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(eventShortDto));

        mockMvc.perform(get("/events")
                        .param("text", "test")
                        .param("categories", "1")
                        .param("paid", "true")
                        .param("from", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getEvents_negativeFrom_throwsBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "-1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_zeroSize_throwsBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEventById_success() throws Exception {
        when(eventService.getPublicEventById(eq(1L), any()))
                .thenReturn(eventFullDto);

        mockMvc.perform(get("/events/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    void getEventById_notFound_throwsNotFound() throws Exception {
        when(eventService.getPublicEventById(eq(999L), any()))
                .thenThrow(new NotFoundException("Event with id 999 not found"));

        mockMvc.perform(get("/events/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}