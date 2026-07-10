package ru.practicum.ewm.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.compilation.service.CompilationService;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.request.service.RequestService;
import ru.practicum.ewm.statistics.StatsClient;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.ewm.user.service.UserService;

public abstract class BaseControllerTest {

    protected MockMvc mockMvc;
    protected ObjectMapper objectMapper;

    // === Сервисы ===
    @MockBean
    protected CategoryService categoryService;

    @MockBean
    protected CompilationService compilationService;

    @MockBean
    protected EventService eventService;

    @MockBean
    protected RequestService requestService;

    @MockBean
    protected UserService userService;

    // === Репозитории ===
    @MockBean
    protected CategoryRepository categoryRepository;

    @MockBean
    protected EventRepository eventRepository;

    @MockBean
    protected RequestRepository requestRepository;

    @MockBean
    protected UserRepository userRepository;

    @MockBean
    protected CompilationRepository compilationRepository;

    // === Клиенты ===
    @MockBean
    protected RestTemplate restTemplate;

    @MockBean
    protected StatsClient statsClient;
}