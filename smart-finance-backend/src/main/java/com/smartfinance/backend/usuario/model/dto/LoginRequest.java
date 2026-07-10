package com.smartfinance.backend.usuario.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no es válido")
        @Size(max = 180, message = "El correo electrónico no puede superar 180 caracteres")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String password,
        /**
         * When {@code true}, the refresh token cookie is issued as a persistent cookie
         * (see {@code UserController#buildRefreshCookie}); when {@code false} or {@code null}
         * (nullable so older clients that omit the field keep the safer default), it is issued
         * as a browser session cookie that disappears once the browser closes.
         */
        Boolean rememberMe
) {
}
