package com.smartfinance.backend.dto.auth;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
