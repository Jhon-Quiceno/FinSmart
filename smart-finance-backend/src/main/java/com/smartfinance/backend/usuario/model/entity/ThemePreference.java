package com.smartfinance.backend.usuario.model.entity;

/**
 * Preferencia de tema visual del usuario, aplicada en la app móvil vía NativeWind. {@code SYSTEM}
 * sigue el tema del sistema operativo; {@code LIGHT}/{@code DARK} lo fuerzan.
 *
 * <p>Persistida como {@code VARCHAR} con un {@code CHECK} constraint (ver
 * {@code V27__add_preferences_to_users.sql}), mismo criterio que {@link
 * com.smartfinance.backend.gastos.model.entity.CategoryType}.
 */
public enum ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}
