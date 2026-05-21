package com.arsh.devsync.service;

import com.arsh.devsync.dto.AuthResponse;
import com.arsh.devsync.dto.LoginRequest;
import com.arsh.devsync.dto.LogoutRequest;
import com.arsh.devsync.dto.RefreshTokenRequest;
import com.arsh.devsync.dto.SignupRequest;
import com.arsh.devsync.entity.RefreshToken;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.exception.DuplicateResourceException;
import com.arsh.devsync.exception.UnauthorizedActionException;
import com.arsh.devsync.repository.UserRepository;
import com.arsh.devsync.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void signup_shouldCreateUserAndReturnTokens_whenEmailIsNew() {
        SignupRequest request = new SignupRequest();
        setField(request, "name", "Arsh");
        setField(request, "email", "arsh@test.com");
        setField(request, "password", "password123");

        User savedUser = new User(
                "Arsh",
                "arsh@test.com",
                "USER",
                "hashedPassword"
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .user(savedUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken("arsh@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

        AuthResponse response = authService.signup(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());

        verify(userRepository).save(any(User.class));
        verify(refreshTokenService).createRefreshToken(savedUser);
    }

    @Test
    void signup_shouldThrowException_whenEmailAlreadyExists() {
        SignupRequest request = new SignupRequest();
        setField(request, "name", "Arsh");
        setField(request, "email", "arsh@test.com");
        setField(request, "password", "password123");

        when(userRepository.findByEmail("arsh@test.com"))
                .thenReturn(Optional.of(mock(User.class)));

        assertThrows(
                DuplicateResourceException.class,
                () -> authService.signup(request)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
    }

    @Test
    void login_shouldReturnTokens_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        setField(request, "email", "arsh@test.com");
        setField(request, "password", "password123");

        User user = new User(
                "Arsh",
                "arsh@test.com",
                "USER",
                "hashedPassword"
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken("arsh@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    void login_shouldThrowException_whenEmailDoesNotExist() {
        LoginRequest request = new LoginRequest();
        setField(request, "email", "wrong@test.com");
        setField(request, "password", "password123");

        when(userRepository.findByEmail("wrong@test.com")).thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedActionException.class,
                () -> authService.login(request)
        );

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_shouldThrowException_whenPasswordIsInvalid() {
        LoginRequest request = new LoginRequest();
        setField(request, "email", "arsh@test.com");
        setField(request, "password", "wrongPassword");

        User user = new User(
                "Arsh",
                "arsh@test.com",
                "USER",
                "hashedPassword"
        );

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThrows(
                UnauthorizedActionException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never()).generateToken(anyString());
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
    }

    @Test
    void refreshToken_shouldRotateTokenAndReturnNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");

        User user = new User(
                "Arsh",
                "arsh@test.com",
                "USER",
                "hashedPassword"
        );

        RefreshToken oldRefreshToken = RefreshToken.builder()
                .token("old-refresh-token")
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token("new-refresh-token")
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenService.verifyRefreshToken("old-refresh-token"))
                .thenReturn(oldRefreshToken);
        when(jwtService.generateToken("arsh@test.com"))
                .thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(newRefreshToken);

        AuthResponse response = authService.refreshToken(request);

        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());

        verify(refreshTokenService).verifyRefreshToken("old-refresh-token");
        verify(refreshTokenService).createRefreshToken(user);
    }

    @Test
    void logout_shouldDeleteRefreshTokenForUser() {
        LogoutRequest request = new LogoutRequest("refresh-token");

        User user = new User(
                "Arsh",
                "arsh@test.com",
                "USER",
                "hashedPassword"
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenService.verifyRefreshToken("refresh-token"))
                .thenReturn(refreshToken);

        authService.logout(request);

        verify(refreshTokenService).deleteByUser(user);
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