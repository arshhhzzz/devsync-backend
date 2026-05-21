package com.arsh.devsync.service;

import com.arsh.devsync.entity.RefreshToken;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.exception.UnauthorizedActionException;
import com.arsh.devsync.repository.RefreshTokenRepository;
import com.arsh.devsync.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        String token = jwtService.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedActionException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedActionException("Refresh token expired");
        }

        return refreshToken;
    }

    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}