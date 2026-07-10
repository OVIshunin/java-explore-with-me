package ru.practicum.ewm.compilation.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompilationMapper {

    private final EventMapper eventMapper;

    public Compilation toEntity(NewCompilationDto dto, List<Event> events) {
        // Добавлена проверка на null для pinned
        boolean pinned = dto.getPinned() != null && dto.getPinned();
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(pinned)
                .events(events != null ? events : List.of())
                .build();
    }

    public CompilationDto toDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(compilation.getEvents() != null
                        ? compilation.getEvents().stream()
                        .map(eventMapper::toShortDto)
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}