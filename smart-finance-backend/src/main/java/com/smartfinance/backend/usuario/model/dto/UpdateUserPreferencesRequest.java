package com.smartfinance.backend.usuario.model.dto;

import com.smartfinance.backend.usuario.model.entity.AppLanguage;
import com.smartfinance.backend.usuario.model.entity.ThemePreference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Payload de {@code PATCH /api/users/preferences} — mismo criterio de "siempre mandar todo" que
 * {@link UpdateProfileRequest}, no un PATCH parcial campo por campo.
 *
 * @param currency código ISO 4217 de 3 letras, acotado al set que hoy soporta la app (ver
 *                 {@code V27__add_preferences_to_users.sql})
 */
public record UpdateUserPreferencesRequest(
        @NotNull(message = "El tema es obligatorio")
        ThemePreference theme,
        @NotNull(message = "La moneda es obligatoria")
        @Pattern(regexp = "COP|USD|MXN|ARS|EUR", message = "Moneda no soportada")
        String currency,
        @NotNull(message = "El idioma es obligatorio")
        AppLanguage language
) {
}
