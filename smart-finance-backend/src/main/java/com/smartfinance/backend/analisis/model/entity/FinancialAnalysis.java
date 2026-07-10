package com.smartfinance.backend.analisis.model.entity;

import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.usuario.model.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A monthly financial snapshot for a {@link User}, computed by
 * {@code FinancialAnalysisService#getSummary}.
 *
 * <p>This is a derived cache/history row, not a source of truth: {@code totalIncome},
 * {@code totalExpense}, {@code topCategory} and the ratios are recomputed from
 * {@link Income}/{@link Expense}/{@link Debt} on every {@code GET /api/analysis/summary} call
 * and then persisted here via an atomic {@code INSERT ... ON CONFLICT (user_id, period_year,
 * period_month) DO UPDATE} (see {@code FinancialAnalysisRepository#upsertSnapshot}), so
 * recalculating a period never creates a duplicate row — it only refreshes the existing one.
 * Only {@link #topCategory} is stored from the top-categories ranking; the full ranking, the
 * 6-month series and the recent-transactions list are computed on demand and not persisted.
 */
@Entity
@Table(name = "financial_analysis")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinancialAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "total_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expense", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalExpense;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal savings;

    @Column(name = "expense_ratio", nullable = false, precision = 8, scale = 4)
    private BigDecimal expenseRatio;

    @Column(name = "debt_ratio", nullable = false, precision = 8, scale = 4)
    private BigDecimal debtRatio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "top_category_id")
    private Category topCategory;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
