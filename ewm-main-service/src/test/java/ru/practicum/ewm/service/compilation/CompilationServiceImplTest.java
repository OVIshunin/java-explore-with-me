package ru.practicum.ewm.service.compilation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;

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
    private Event event;

    @BeforeEach
    void setUp() {
        event = Event.builder().id(1L).title("Test Event").build();

        compilation = Compilation.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of(event))
                .build();

        compilationDto = CompilationDto.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .build();

        newCompilationDto = NewCompilationDto.builder()
                .title("Test Compilation")
                .pinned(true)
                .events(List.of(1L))
                .build();
    }

    @Test
    void createCompilation_success() {
        when(compilationRepository.existsByTitle("Test Compilation")).thenReturn(false);
        when(eventRepository.findAllById(List.of(1L))).thenReturn(List.of(event));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Compilation");
    }

    @Test
    void createCompilation_duplicateTitle_throwsConflictException() {
        when(compilationRepository.existsByTitle("Test Compilation")).thenReturn(true);

        assertThatThrownBy(() -> compilationService.createCompilation(newCompilationDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Compilation with title 'Test Compilation' already exists");
    }

    @Test
    void createCompilation_withoutEvents_success() {
        newCompilationDto.setEvents(null);

        when(compilationRepository.existsByTitle("Test Compilation")).thenReturn(false);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        assertThat(result).isNotNull();
    }

    @Test
    void deleteCompilation_success() {
        when(compilationRepository.existsById(1L)).thenReturn(true);

        compilationService.deleteCompilation(1L);

        verify(compilationRepository).deleteById(1L);
    }

    @Test
    void deleteCompilation_notFound_throwsNotFoundException() {
        when(compilationRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> compilationService.deleteCompilation(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation with id 1 not found");
    }

    @Test
    void updateCompilation_success() {
        UpdateCompilationRequest request = UpdateCompilationRequest.builder()
                .title("Updated Title")
                .pinned(false)
                .events(List.of(1L))
                .build();

        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(eventRepository.findAllById(List.of(1L))).thenReturn(List.of(event));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

        CompilationDto result = compilationService.updateCompilation(1L, request);

        assertThat(result).isNotNull();
    }

    @Test
    void updateCompilation_notFound_throwsNotFoundException() {
        UpdateCompilationRequest request = UpdateCompilationRequest.builder().build();

        when(compilationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.updateCompilation(1L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getCompilations_withPinned_success() {
        Page<Compilation> compilationPage = new PageImpl<>(List.of(compilation));

        when(compilationRepository.findByPinned(true, Pageable.ofSize(10))).thenReturn(compilationPage);
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

        List<CompilationDto> result = compilationService.getCompilations(true, 0, 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void getCompilations_withoutPinned_success() {
        Page<Compilation> compilationPage = new PageImpl<>(List.of(compilation));

        when(compilationRepository.findAll(any(Pageable.class))).thenReturn(compilationPage);
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

        List<CompilationDto> result = compilationService.getCompilations(null, 0, 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void getCompilationById_success() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

        CompilationDto result = compilationService.getCompilationById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getCompilationById_notFound_throwsNotFoundException() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.getCompilationById(1L))
                .isInstanceOf(NotFoundException.class);
    }
}