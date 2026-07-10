package com.smartfinance.backend.analisis.repository;

import com.smartfinance.backend.analisis.model.entity.FinancialAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Persistence access for {@link FinancialAnalysis}, always scoped by owner.
 */
public interface FinancialAnalysisRepository extends JpaRepository<FinancialAnalysis, Long> {

    Optional<FinancialAnalysis> findByUser_IdAndPeriodYearAndPeriodMonth(
            Long userId, Integer periodYear, Integer periodMonth
    );

    /**
     * Atomically inserts or refreshes the monthly snapshot for {@code (userId, periodYear,
     * periodMonth)} in a single database round trip, instead of a read-then-decide-insert-or-
     * update sequence. This relies on {@code uk_financial_analysis_user_period} (see
     * {@code V6__create_financial_analysis_table.sql}) as the conflict target, so recalculating
     * a period that was already snapshotted (e.g. the user opens the dashboard twice in the
     * same month) refreshes the existing row instead of violating the unique constraint or
     * racing a concurrent request into a duplicate row.
     */
    @Modifying
    @Query(value = "INSERT INTO financial_analysis "
            + "(user_id, period_year, period_month, total_income, total_expense, savings, "
            + "expense_ratio, debt_ratio, top_category_id, created_at, updated_at) "
            + "VALUES (:userId, :periodYear, :periodMonth, :totalIncome, :totalExpense, :savings, "
            + ":expenseRatio, :debtRatio, :topCategoryId, now(), now()) "
            + "ON CONFLICT (user_id, period_year, period_month) DO UPDATE SET "
            + "total_income = EXCLUDED.total_income, "
            + "total_expense = EXCLUDED.total_expense, "
            + "savings = EXCLUDED.savings, "
            + "expense_ratio = EXCLUDED.expense_ratio, "
            + "debt_ratio = EXCLUDED.debt_ratio, "
            + "top_category_id = EXCLUDED.top_category_id, "
            + "updated_at = now()",
            nativeQuery = true)
    void upsertSnapshot(
            @Param("userId") Long userId,
            @Param("periodYear") Integer periodYear,
            @Param("periodMonth") Integer periodMonth,
            @Param("totalIncome") BigDecimal totalIncome,
            @Param("totalExpense") BigDecimal totalExpense,
            @Param("savings") BigDecimal savings,
            @Param("expenseRatio") BigDecimal expenseRatio,
            @Param("debtRatio") BigDecimal debtRatio,
            @Param("topCategoryId") Long topCategoryId
    );
}
