package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Expense;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link Expense}, always scoped by owner.
 *
 * <p>Dynamic filters (category/date range/payment method) are built via
 * {@link com.smartfinance.backend.repository.specification.ExpenseSpecifications} instead of a
 * static {@code @Query}, since a JPQL {@code :param IS NULL OR ...} pattern fails against
 * PostgreSQL when the parameter is null (the driver cannot infer its type).
 *
 * <p>{@link #sumAmountByUserAndPeriod}, {@link #findTopCategoriesByUserAndPeriod} and
 * {@link #findRecentByUserId} back {@code FinancialAnalysisService} (Sprint 4) and are plain
 * {@code @Query}/derived methods rather than
 * {@link org.springframework.data.jpa.domain.Specification} builders, since their filters
 * (userId + a fixed date range) are never optional.
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByIdAndUser_Id(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e "
            + "WHERE e.user.id = :userId AND e.date >= :start AND e.date <= :end")
    BigDecimal sumAmountByUserAndPeriod(
            @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end
    );

    @Query("SELECT e.category.id AS categoryId, e.category.name AS categoryName, SUM(e.amount) AS total "
            + "FROM Expense e WHERE e.user.id = :userId AND e.date >= :start AND e.date <= :end "
            + "GROUP BY e.category.id, e.category.name "
            + "ORDER BY SUM(e.amount) DESC")
    List<CategoryTotalProjection> findTopCategoriesByUserAndPeriod(
            @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end
    );

    /**
     * {@code LEFT JOIN FETCH category} so rendering {@code category.name} for each of the (at
     * most 10) rows returned doesn't trigger a lazy-load per row — without it, building the
     * "recent transactions" list is an N+1 (one extra SELECT per row) on every
     * {@code GET /api/analysis/summary} call.
     */
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.category WHERE e.user.id = :userId "
            + "ORDER BY e.date DESC, e.id DESC")
    List<Expense> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Every expense for the user in {@code [start, end]}, with category eagerly fetched (same
     * N+1 rationale as {@link #findRecentByUserId}), ordered by date descending. Backs
     * {@code ReportService#getMovements} (Sprint 6), which needs every movement in the period
     * rather than a fixed-size "recent" window.
     */
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.category WHERE e.user.id = :userId "
            + "AND e.date >= :start AND e.date <= :end ORDER BY e.date DESC, e.id DESC")
    List<Expense> findByUserAndPeriod(
            @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end
    );

    /**
     * Distinct user ids with at least one expense in {@code [start, end]}, backing
     * {@code WeeklySummaryJob} (Sprint 5, Batch 3). Used together with
     * {@link IncomeRepository#findDistinctUserIdsByDateBetween} to build the set of users with
     * any activity in the period, avoiding a per-user existence check across every registered
     * user.
     */
    @Query("SELECT DISTINCT e.user.id FROM Expense e WHERE e.date >= :start AND e.date <= :end")
    List<Long> findDistinctUserIdsByDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Most recent expense date per user, across every user, in a single grouped query. Backs
     * {@code InactivityReminderJob} (Sprint 5, Batch 3), which combines this with
     * {@link IncomeRepository#findLatestIncomeDatePerUser()} in memory instead of querying each
     * user individually.
     */
    @Query("SELECT e.user.id AS userId, MAX(e.date) AS lastDate FROM Expense e GROUP BY e.user.id")
    List<UserLastActivityProjection> findLatestExpenseDatePerUser();
}
