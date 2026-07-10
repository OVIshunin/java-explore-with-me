package ru.practicum.ewm.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.test.BaseControllerTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final Location location = Location.builder()
            .lat(55.75)
            .lon(37.62)
            .build();

    private final LocalDateTime eventDate = LocalDateTime.now().plusHours(3);

    // ==================== PRIVATE ====================

    @Test
    void getUserEvents_shouldReturnList() throws Exception {
        EventShortDto dto = EventShortDto.builder()
                .id(1L)
                .title("Событие")
                .build();

        when(eventService.getUserEvents(1L, 0, 10)).thenReturn(List.of(dto));

        mockMvc.perform(get("/users/1/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createEvent_shouldReturnCreated() throws Exception {
        NewEventDto input = NewEventDto.builder()
                .annotation("Аннотация события длиной не менее 20 символов")
                .category(1L)
                .description("Описание события длиной не менее 20 символов")
                .eventDate(eventDate)
                .location(location)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .title("Заголовок")
                .build();

        EventFullDto output = EventFullDto.builder()
                .id(1L)
                .state(EventState.PENDING)
                .build();

        when(eventService.createEvent(eq(1L), any(NewEventDto.class))).thenReturn(output);

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.state").value("PENDING"));
    }

    @Test
    void createEvent_shouldReturnBadRequest_whenAnnotationTooShort() throws Exception {
        NewEventDto input = NewEventDto.builder()
                .annotation("Слишком короткая")
                .category(1L)
                .description("Описание события длиной не менее 20 символов")
                .eventDate(eventDate)
                .location(location)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .title("Заголовок")
                .build();

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_shouldReturnBadRequest_whenEventDateInPast() throws Exception {
        NewEventDto input = NewEventDto.builder()
                .annotation("Аннотация события длиной не менее 20 символов")
                .category(1L)
                .description("Описание события длиной не менее 20 символов")
                .eventDate(LocalDateTime.now().minusHours(1))
                .location(location)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .title("Заголовок")
                .build();

        when(eventService.createEvent(eq(1L), any(NewEventDto.class)))
                .thenThrow(new ValidationException("Event date must be at least 2 hours from now"));

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserEventById_shouldReturnEvent() throws Exception {
        EventFullDto dto = EventFullDto.builder()
                .id(1L)
                .title("Событие")
                .build();

        when(eventService.getUserEventById(1L, 1L)).thenReturn(dto);

        mockMvc.perform(get("/users/1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getUserEventById_shouldReturnNotFound() throws Exception {
        when(eventService.getUserEventById(1L, 999L))
                .thenThrow(new NotFoundException("Event not found"));

        mockMvc.perform(get("/users/1/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserEvent_shouldReturnUpdated() throws Exception {
        UpdateEventUserRequest input = UpdateEventUserRequest.builder()
                .title("Новый заголовок")
                .build();

        EventFullDto output = EventFullDto.builder()
                .id(1L)
                .title("Новый заголовок")
                .build();

        when(eventService.updateUserEvent(eq(1L), eq(1L), any(UpdateEventUserRequest.class)))
                .thenReturn(output);

        mockMvc.perform(patch("/users/1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Новый заголовок"));
    }

    // ==================== PUBLIC ====================

    @Test
    void getPublicEvents_shouldReturnList() throws Exception {
        EventShortDto dto = EventShortDto.builder()
                .id(1L)
                .title("Публичное событие")
                .build();

        when(eventService.getPublicEvents(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPublicEventById_shouldReturnEvent() throws Exception {
        EventFullDto dto = EventFullDto.builder()
                .id(1L)
                .title("Публичное событие")
                .build();

        when(eventService.getPublicEventById(1L)).thenReturn(dto);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getPublicEventById_shouldReturnNotFound() throws Exception {
        when(eventService.getPublicEventById(999L))
                .thenThrow(new NotFoundException("Event not found"));

        mockMvc.perform(get("/events/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== ADMIN ====================

    @Test
    void getAdminEvents_shouldReturnList() throws Exception {
        EventFullDto dto = EventFullDto.builder()
                .id(1L)
                .title("Событие")
                .build();

        when(eventService.getAdminEvents(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateAdminEvent_shouldReturnUpdated() throws Exception {
        UpdateEventAdminRequest input = UpdateEventAdminRequest.builder()
                .title("Новый заголовок от админа")
                .build();

        EventFullDto output = EventFullDto.builder()
                .id(1L)
                .title("Новый заголовок от админа")
                .build();

        when(eventService.updateAdminEvent(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenReturn(output);

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Новый заголовок от админа"));
    }

    @Test
    void updateAdminEvent_shouldReturnNotFound() throws Exception {
        UpdateEventAdminRequest input = UpdateEventAdminRequest.builder()
                .title("Новый заголовок")
                .build();

        when(eventService.updateAdminEvent(eq(999L), any(UpdateEventAdminRequest.class)))
                .thenThrow(new NotFoundException("Event not found"));

        mockMvc.perform(patch("/admin/events/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }
}
