package com.arsh.devsync.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}