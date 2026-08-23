package com.smartfinance.backend.usuario.model.entity;

/**
 * Idioma preferido del usuario. Persistido y expuesto por {@code /api/users/preferences}, pero la
 * app todavía no tiene una capa de internacionalización que lea este valor para traducir su UI
 * (todos los textos son literales en español hoy) — ver M3 en
 * {@code docs/plan-sprints-movil-nativo.md}. Elegir {@code EN} guarda la preferencia sin cambiar
 * ningún texto visible todavía.
 *
 * <p>Persistida como {@code VARCHAR} con un {@code CHECK} constraint (ver
 * {@code V27__add_preferences_to_users.sql}), mismo criterio que {@link
 * com.smartfinance.backend.gastos.model.entity.CategoryType}.
 */
public enum AppLanguage {
    ES,
    EN
}
