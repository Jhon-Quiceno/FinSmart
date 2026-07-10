package com.smartfinance.backend.usuario.model.dto;

public record UserResponse(
        Long id,
        String name,
        String email
) {
}
