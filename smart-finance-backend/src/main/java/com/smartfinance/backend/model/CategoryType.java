package com.smartfinance.backend.model;

/**
 * Classifies a {@link Category} as either an income or an expense category.
 *
 * <p>Persisted as a {@code VARCHAR} column (see {@code V3__create_categories_incomes_expenses.sql})
 * rather than a native PostgreSQL enum type, matching the rest of the project's simple
 * varchar-plus-{@code CHECK}-constraint approach to enumerations.
 */
public enum CategoryType {
    INCOME,
    EXPENSE
}
