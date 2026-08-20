package com.smartfinance.backend.usuario.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code refreshToken} is only populated for mobile clients (see
 * {@code UserController}'s {@code X-Client: mobile} handling) — browser clients keep getting the
 * refresh token exclusively via the {@code HttpOnly} cookie, never in the body.
 * {@link JsonInclude.Include#NON_NULL} keeps the field out of the JSON entirely for the browser
 * case instead of serializing it as {@code "refreshToken":null}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        String refreshToken
) {
}
