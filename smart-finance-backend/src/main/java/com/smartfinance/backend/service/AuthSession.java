package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.auth.AuthResponse;

public record AuthSession(
        AuthResponse response,
        String refreshToken,
        boolean rememberMe
) {
}
