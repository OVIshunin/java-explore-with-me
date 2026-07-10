package ru.practicum.ewm.compilation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CompilationMapper compilationMapper;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private CompilationDto compilationDto;
    private NewCompilationDto newCompilationDto;

    @BeforeEach
    void setUp() {
        Event event = Event.builder()
                .id(1L)
                .title("Событие")
                .build();

        compilation = Compilation.builder()
                .id(1L)
                .title("Летние концерты")
                .pinned(true)
                .events(List.of(event))
                .build();

        compilationDto = CompilationDto.builder()
                .id(1L)
                .title("Летние концерты")
                .pinned(true)
                .build();

        newCompilationDto = NewCompilationDto.builder()
                .title("Летние концерты")
                .pinned(true)
                .events(List.of(1L))
                .build();
    }

    // ==================== createCompilation ====================

    @Test
    void createCompilation_shouldSaveAndReturnCompilation() {
        when(eventRepository.findAllById(List.of(1L))).thenReturn(List.of(Event.builder().id(1L).build()));
        when(compilationMapper.toEntity(any(NewCompilationDto.class), anyList()))
                .thenReturn(compilation);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);

        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Летние концерты");
        verify(compilationRepository).save(any(Compilation.class));
    }

    @Test
    void createCompilation_shouldCreateCompilationWithoutEvents() {
        NewCompilationDto emptyEventsDto = NewCompilationDto.builder()
                .title("Пустая подборка")
                .pinned(false)
                .events(List.of())
                .build();

        Compilation emptyCompilation = Compilation.builder()
                .id(2L)
                .title("Пустая подборка")
                .pinned(false)
                .events(List.of())
                .build();

        when(compilationMapper.toEntity(any(NewCompilationDto.class), anyList()))
                .thenReturn(emptyCompilation);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(emptyCompilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(CompilationDto.builder()
                .id(2L)
                .title("Пустая подборка")
                .pinned(false)
                .build());

        CompilationDto result = compilationService.createCompilation(emptyEventsDto);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Пустая подборка");
        verify(compilationRepository).save(any(Compilation.class));
    }

    // ==================== deleteCompilation ====================

    @Test
    void deleteCompilation_shouldDeleteCompilation_whenExists() {
        when(compilationRepository.existsById(1L)).thenReturn(true);

        compilationService.deleteCompilation(1L);

        verify(compilationRepository).deleteById(1L);
    }

    @Test
    void deleteCompilation_shouldThrowNotFoundException_whenNotFound() {
        when(compilationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> compilationService.deleteCompilation(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation not found");
    }

    // ==================== updateCompilation ====================

    @Test
    void updateCompilation_shouldUpdateTitleAndPinned() {
        UpdateCompilationRequest updateRequest = UpdateCompilationRequest.builder()
                .title("Зимние концерты")
                .pinned(false)
                .build();

        Compilation updatedCompilation = Compilation.builder()
                .id(1L)
                .title("Зимние концерты")
                .pinned(false)
                .events(List.of())
                .build();

        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(updatedCompilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(CompilationDto.builder()
                .id(1L)
                .title("Зимние концерты")
                .pinned(false)
                .build());

        CompilationDto result = compilationService.updateCompilation(1L, updateRequest);

        assertThat(result.getTitle()).isEqualTo("Зимние концерты");
        assertThat(result.getPinned()).isFalse();
        verify(compilationRepository).save(any(Compilation.class));
    }

    @Test
    void updateCompilation_shouldUpdateEvents() {
        UpdateCompilationRequest updateRequest = UpdateCompilationRequest.builder()
                .events(List.of(2L, 3L))
                .build();

        Compilation updatedCompilation = Compilation.builder()
                .id(1L)
                .title("Летние концерты")
                .pinned(true)
                .events(List.of(Event.builder().id(2L).build(), Event.builder().id(3L).build()))
                .build();

        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(eventRepository.findAllById(List.of(2L, 3L))).thenReturn(List.of(
                Event.builder().id(2L).build(),
                Event.builder().id(3L).build()
        ));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(updatedCompilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);

        CompilationDto result = compilationService.updateCompilation(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(eventRepository).findAllById(List.of(2L, 3L));
    }

    @Test
    void updateCompilation_shouldThrowNotFoundException_whenCompilationNotFound() {
        when(compilationRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateCompilationRequest updateRequest = UpdateCompilationRequest.builder()
                .title("Новый заголовок")
                .build();

        assertThatThrownBy(() -> compilationService.updateCompilation(999L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation not found");
    }

    // ==================== getCompilations ====================

    @Test
    void getCompilations_shouldReturnAllCompilations() {
        when(compilationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(compilation)));
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);

        List<CompilationDto> result = compilationService.getCompilations(null, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Летние концерты");
    }


    @Test
    void getCompilations_shouldReturnEmptyList_whenNoCompilations() {
        when(compilationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<CompilationDto> result = compilationService.getCompilations(null, 0, 10);

        assertThat(result).isEmpty();
    }

    // ==================== getCompilationById ====================

    @Test
    void getCompilationById_shouldReturnCompilation() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);

        CompilationDto result = compilationService.getCompilationById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Летние концерты");
    }

    @Test
    void getCompilationById_shouldThrowNotFoundException_whenNotFound() {
        when(compilationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.getCompilationById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation not found");
    }
}