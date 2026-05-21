package com.arsh.devsync.service;

import com.arsh.devsync.entity.RefreshToken;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.exception.UnauthorizedActionException;
import com.arsh.devsync.repository.RefreshTokenRepository;
import com.arsh.devsync.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                refreshTokenService,
                "refreshExpiration",
                604800000L
        );
    }

    @Test
    void createRefreshToken_shouldDeleteOldTokenAndCreateNewToken() {
        User user = new User("Arsh", "arsh@test.com", "USER", "hashedPassword");

        when(jwtService.generateRefreshToken("arsh@test.com"))
                .thenReturn("refresh-token");

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertEquals("refresh-token", result.getToken());
        assertEquals(user, result.getUser());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getExpiresAt());

        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void verifyRefreshToken_shouldReturnToken_whenTokenIsValid() {
        User user = new User("Arsh", "arsh@test.com", "USER", "hashedPassword");

        RefreshToken refreshToken = RefreshToken.builder()
                .token("valid-refresh-token")
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.verifyRefreshToken(
                "valid-refresh-token"
        );

        assertEquals("valid-refresh-token", result.getToken());
        assertEquals(user, result.getUser());

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyRefreshToken_shouldThrowException_whenTokenDoesNotExist() {
        when(refreshTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedActionException.class,
                () -> refreshTokenService.verifyRefreshToken("invalid-token")
        );

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyRefreshToken_shouldDeleteAndThrowException_whenTokenExpired() {
        User user = new User("Arsh", "arsh@test.com", "USER", "hashedPassword");

        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired-token")
                .user(user)
                .createdAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(expiredToken));

        assertThrows(
                UnauthorizedActionException.class,
                () -> refreshTokenService.verifyRefreshToken("expired-token")
        );

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void deleteByUser_shouldDeleteRefreshTokenForUser() {
        User user = new User("Arsh", "arsh@test.com", "USER", "hashedPassword");

        refreshTokenService.deleteByUser(user);

        verify(refreshTokenRepository).deleteByUser(user);
    }
}