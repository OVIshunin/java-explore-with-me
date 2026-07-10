package ru.practicum.ewm.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        userDto = UserDto.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();
    }

    // ==================== getUsers ====================

    @Test
    void getUsers_shouldReturnAllUsers_whenIdsAreNull() {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toDto(user)).thenReturn(userDto);

        List<UserDto> result = userService.getUsers(null, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("john@example.com");
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void getUsers_shouldReturnUsersByIds_whenIdsProvided() {
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        List<UserDto> result = userService.getUsers(List.of(1L), 0, 10);

        assertThat(result).hasSize(1);
        verify(userRepository).findAllById(List.of(1L));
    }

    @Test
    void getUsers_shouldReturnEmptyList_whenNoUsers() {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<UserDto> result = userService.getUsers(null, 0, 10);

        assertThat(result).isEmpty();
    }

    // ==================== createUser ====================

    @Test
    void createUser_shouldSaveAndReturnUser() {
        when(userMapper.toEntity(userDto)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = userService.createUser(userDto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowConflictException_whenEmailAlreadyExists() {
        UserDto existingUserDto = UserDto.builder()
                .name("Jane")
                .email("john@example.com")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(existingUserDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== deleteUser ====================

    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrowNotFoundException_whenUserDoesNotExist() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id 999 not found");
    }
}
