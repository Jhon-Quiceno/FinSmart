package com.smartfinance.backend.event;

/**
 * Published by {@code ExpenseService#createExpense} after a new {@code Expense} is persisted.
 *
 * <p>Consumed by {@code OverspendAlertListener} (Sprint 5, Batch 3) to recompute the current
 * month's expense-to-income ratio for {@code userId} and notify when it crosses the overspend
 * threshold, replacing the equivalent n8n workflow (see {@code docs/sprints/sprint5.md}).
 *
 * <p>Deliberately carries only ids, not the full {@code Expense}/{@code ExpenseResponse}: the
 * listener recomputes the current aggregate from the database rather than trusting a snapshot
 * passed at publish time, since other expenses may have been created concurrently.
 *
 * @param userId    owner of the created expense
 * @param expenseId id of the created expense
 */
public record ExpenseCreatedEvent(Long userId, Long expenseId) {
}
