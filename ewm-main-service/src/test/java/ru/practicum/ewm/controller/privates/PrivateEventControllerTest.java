package ru.practicum.ewm.controller.privates;

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
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.service.event.EventService;
import ru.practicum.ewm.service.request.RequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private RequestService requestService;

    private EventFullDto eventFullDto;
    private NewEventDto newEventDto;

    @BeforeEach
    void setUp() {
        LocationDto locationDto = LocationDto.builder()
                .lat(55.7558f)
                .lon(37.6173f)
                .build();

        newEventDto = NewEventDto.builder()
                .annotation("Test Annotation for event")
                .category(1L)
                .description("Test description for event that is long enough")
                .eventDate(LocalDateTime.now().plusHours(3))
                .location(locationDto)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .title("Test Event")
                .build();

        eventFullDto = EventFullDto.builder()
                .id(1L)
                .annotation("Test Annotation for event")
                .title("Test Event")
                .build();
    }

    @Test
    void createEvent_success() throws Exception {
        when(eventService.createEvent(eq(1L), any(NewEventDto.class)))
                .thenReturn(eventFullDto);

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getUserEvents_success() throws Exception {
        when(eventService.getUserEvents(eq(1L), anyInt(), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get("/users/1/events")
                        .param("from", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getUserEventById_success() throws Exception {
        when(eventService.getUserEventById(eq(1L), eq(1L)))
                .thenReturn(eventFullDto);

        mockMvc.perform(get("/users/1/events/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void updateUserEvent_success() throws Exception {
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .title("Updated Title")
                .build();

        when(eventService.updateUserEvent(eq(1L), eq(1L), any(UpdateEventUserRequest.class)))
                .thenReturn(eventFullDto);

        mockMvc.perform(patch("/users/1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}