package com.smartfinance.backend.usuario.model.dto;

import com.smartfinance.backend.usuario.model.entity.AppLanguage;
import com.smartfinance.backend.usuario.model.entity.ThemePreference;

/**
 * Respuesta de {@code GET /api/users/preferences} y {@code PATCH /api/users/preferences} (M3 del
 * track móvil, ver {@code docs/plan-sprints-movil-nativo.md}).
 */
public record UserPreferencesResponse(
        ThemePreference theme,
        String currency,
        AppLanguage language
) {
}
