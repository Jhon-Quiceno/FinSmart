package com.smartfinance.backend.usuario.service;

import com.smartfinance.backend.usuario.model.dto.AuthResponse;

public record AuthSession(
        AuthResponse response,
        String refreshToken,
        boolean rememberMe
) {
}
