package com.smartfinance.backend.ingresos.repository;

import com.smartfinance.backend.common.repository.UserLastActivityProjection;
import com.smartfinance.backend.ingresos.model.entity.Income;
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
 * Persistence access for {@link Income}, always scoped by owner.
 *
 * <p>Dynamic filters (period) are built via
 * {@link com.smartfinance.backend.ingresos.repository.specification.IncomeSpecifications} instead of a
 * static {@code @Query}, since a JPQL {@code :param IS NULL OR ...} pattern fails against
 * PostgreSQL when the parameter is null (the driver cannot infer its type).
 *
 * <p>{@link #sumAmountByUserAndPeriod} and {@link #findRecentByUserId} back
 * {@code FinancialAnalysisService} (Sprint 4); their filters (userId + a fixed date range) are
 * never optional, so a plain {@code @Query}/derived method is used instead of a
 * {@link org.springframework.data.jpa.domain.Specification} builder.
 */
public interface IncomeRepository extends JpaRepository<Income, Long>, JpaSpecificationExecutor<Income> {

    Optional<Income> findByIdAndUser_Id(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i "
            + "WHERE i.user.id = :userId AND i.date >= :start AND i.date <= :end")
    BigDecimal sumAmountByUserAndPeriod(
            @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end
    );

    /**
     * {@code LEFT JOIN FETCH category} so rendering {@code category.name} for each of the (at
     * most 10) rows returned doesn't trigger a lazy-load per row — without it, building the
     * "recent transactions" list is an N+1 (one extra SELECT per row) on every
     * {@code GET /api/analysis/summary} call.
     */
    @Query("SELECT i FROM Income i LEFT JOIN FETCH i.category WHERE i.user.id = :userId "
            + "ORDER BY i.date DESC, i.id DESC")
    List<Income> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Every income for the user in {@code [start, end]}, with category eagerly fetched (same
     * N+1 rationale as {@link #findRecentByUserId}), ordered by date descending. Backs
     * {@code ReportService#getMovements} (Sprint 6), which needs every movement in the period
     * rather than a fixed-size "recent" window.
     */
    @Query("SELECT i FROM Income i LEFT JOIN FETCH i.category WHERE i.user.id = :userId "
            + "AND i.date >= :start AND i.date <= :end ORDER BY i.date DESC, i.id DESC")
    List<Income> findByUserAndPeriod(
            @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end
    );

    /**
     * Distinct user ids with at least one income in {@code [start, end]}, backing
     * {@code WeeklySummaryJob} (Sprint 5, Batch 3). See
     * {@link ExpenseRepository#findDistinctUserIdsByDateBetween} for the counterpart.
     */
    @Query("SELECT DISTINCT i.user.id FROM Income i WHERE i.date >= :start AND i.date <= :end")
    List<Long> findDistinctUserIdsByDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Most recent income date per user, across every user, in a single grouped query. Backs
     * {@code InactivityReminderJob} (Sprint 5, Batch 3); see
     * {@link ExpenseRepository#findLatestExpenseDatePerUser()} for the counterpart.
     */
    @Query("SELECT i.user.id AS userId, MAX(i.date) AS lastDate FROM Income i GROUP BY i.user.id")
    List<UserLastActivityProjection> findLatestIncomeDatePerUser();
}
