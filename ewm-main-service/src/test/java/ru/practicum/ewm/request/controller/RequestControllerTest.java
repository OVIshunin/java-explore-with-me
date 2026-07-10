package ru.practicum.ewm.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.test.BaseControllerTest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
class RequestControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getUserRequests_shouldReturnList() throws Exception {
        ParticipationRequestDto dto = ParticipationRequestDto.builder()
                .id(1L)
                .status(RequestStatus.PENDING)
                .build();

        when(requestService.getUserRequests(1L)).thenReturn(java.util.List.of(dto));

        mockMvc.perform(get("/users/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createRequest_shouldReturnCreated() throws Exception {
        ParticipationRequestDto dto = ParticipationRequestDto.builder()
                .id(1L)
                .status(RequestStatus.PENDING)
                .build();

        when(requestService.createRequest(1L, 1L)).thenReturn(dto);

        mockMvc.perform(post("/users/1/requests")
                        .param("eventId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void createRequest_shouldReturnConflict_whenDuplicate() throws Exception {
        when(requestService.createRequest(1L, 1L))
                .thenThrow(new ConflictException("Request already exists"));

        mockMvc.perform(post("/users/1/requests")
                        .param("eventId", "1"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelRequest_shouldReturnOk() throws Exception {
        ParticipationRequestDto dto = ParticipationRequestDto.builder()
                .id(1L)
                .status(RequestStatus.CANCELED)
                .build();

        when(requestService.cancelRequest(1L, 1L)).thenReturn(dto);

        mockMvc.perform(patch("/users/1/requests/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancelRequest_shouldReturnNotFound() throws Exception {
        when(requestService.cancelRequest(1L, 999L))
                .thenThrow(new NotFoundException("Request not found"));

        mockMvc.perform(patch("/users/1/requests/999/cancel"))
                .andExpect(status().isNotFound());
    }
}