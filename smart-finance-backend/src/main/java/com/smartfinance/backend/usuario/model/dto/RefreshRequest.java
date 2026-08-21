package com.smartfinance.backend.usuario.model.dto;

/**
 * Optional JSON body for {@code POST /api/users/refresh} and {@code /api/users/logout}.
 *
 * <p>Only clients that receive the refresh token in the response body use this (see
 * {@code UserController.isMobileClient}, driven by the {@code X-Client: mobile} header). A
 * browser keeps sending the token via the {@code HttpOnly} cookie and never sends a body — the
 * field has no {@code @NotBlank} because "no token at all" is not a validation error here, it's
 * an authentication failure already handled by {@code UserService.refresh} throwing
 * {@code InvalidRefreshTokenException} (mapped to 401) when it receives {@code null}.
 */
public record RefreshRequest(String refreshToken) {
}
