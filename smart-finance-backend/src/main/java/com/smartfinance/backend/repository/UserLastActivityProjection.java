package com.smartfinance.backend.repository;

import java.time.LocalDate;

/**
 * Projection of the most recent activity date for one user, grouped across every row they own,
 * as returned by {@link ExpenseRepository#findLatestExpenseDatePerUser()} and
 * {@link IncomeRepository#findLatestIncomeDatePerUser()}.
 *
 * <p>Backs {@code InactivityReminderJob} (Sprint 5, Batch 3), which combines both projections in
 * memory instead of querying per user, so the job stays cheap regardless of how many users the
 * platform has.
 */
public interface UserLastActivityProjection {

    Long getUserId();

    LocalDate getLastDate();
}
