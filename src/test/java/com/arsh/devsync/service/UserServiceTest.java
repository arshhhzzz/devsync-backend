package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateUserRequest;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldSaveAndReturnUser() {
        CreateUserRequest request = new CreateUserRequest();
        setField(request, "name", "Arsh");
        setField(request, "email", "arsh@test.com");
        setField(request, "role", "USER");

        User savedUser = new User("Arsh", "arsh@test.com", "USER");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.createUser(request);

        assertEquals("Arsh", result.getName());
        assertEquals("arsh@test.com", result.getEmail());
        assertEquals("USER", result.getRole());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void getAllUsers_shouldReturnUsers() {
        User user = new User("Arsh", "arsh@test.com", "USER");

        when(userRepository.findAll()).thenReturn(List.of(user));

        List result = userService.getAllUsers();

        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        User user = new User("Arsh", "arsh@test.com", "USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertEquals("Arsh", result.getName());
    }

    @Test
    void getUserById_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(99L)
        );
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}