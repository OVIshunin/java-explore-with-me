package ru.practicum.ewm.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.model.Location;

@Component
public class LocationMapper {

    public Location toEntity(LocationDto dto) {
        if (dto == null) {
            return null;
        }

        return Location.builder()
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();
    }

    public LocationDto toDto(Location location) {
        if (location == null) {
            return null;
        }

        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}