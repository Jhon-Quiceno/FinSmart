package com.smartfinance.backend.deudas.repository;

import com.smartfinance.backend.deudas.model.entity.Debt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link Debt}, always scoped by owner.
 */
public interface DebtRepository extends JpaRepository<Debt, Long> {

    Optional<Debt> findByIdAndUser_Id(Long id, Long userId);

    Page<Debt> findAllByUser_Id(Long userId, Pageable pageable);

    /**
     * Unpaged variant of {@link #findAllByUser_Id(Long, Pageable)}, used by
     * {@code com.smartfinance.backend.ia.service.ai.FinancialContextBuilder} to list every debt for
     * the AI system prompt (filtering to those with a positive {@code remainingAmount} happens
     * in the caller, since there is no {@code isActive} flag on {@link Debt} — see the class
     * Javadoc).
     */
    List<Debt> findAllByUser_Id(Long userId);

    /**
     * Atomically decrements {@code remainingAmount}, guarded by the {@code WHERE} clause so the
     * decrement and the "amount does not exceed the remaining balance" check happen as a single
     * database operation instead of read-then-write. This closes the lost-update race where two
     * concurrent {@code DebtPaymentService#createPayment} calls both read the same
     * {@code remainingAmount}, both pass an in-memory validation, and the second {@code save()}
     * silently overwrites the first's decrement (the {@code CHECK (remaining_amount >= 0)}
     * constraint does not catch this, since each independently-computed final value can still
     * individually satisfy it).
     *
     * @return {@code 1} if the decrement was applied, {@code 0} if {@code amount} exceeds the
     *         debt's current remaining balance — callers must treat {@code 0} as a rejected
     *         payment and must not persist a {@link com.smartfinance.backend.deudas.model.entity.DebtPayment}.
     */
    @Modifying
    @Query("UPDATE Debt d SET d.remainingAmount = d.remainingAmount - :amount "
            + "WHERE d.id = :id AND d.remainingAmount >= :amount")
    int decrementRemainingAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * Sums {@code remainingAmount} across every debt owned by the user, used by
     * {@code FinancialAnalysisService} as "total debt" for the debt-ratio calculation. There is
     * no {@code status}/{@code isActive} flag on {@link Debt} (see the class Javadoc) — a debt
     * with {@code remainingAmount = 0} contributes {@code 0} here, which is the correct "no
     * longer active" behavior without needing a separate filter.
     */
    @Query("SELECT COALESCE(SUM(d.remainingAmount), 0) FROM Debt d WHERE d.user.id = :userId")
    BigDecimal sumRemainingAmountByUser(@Param("userId") Long userId);

    /**
     * Scans every debt (across all users) with a positive {@code remainingAmount} whose
     * {@code dueDate} falls within {@code [start, end]}, backing {@code PaymentReminderJob}
     * (Sprint 5, Batch 3). {@code JOIN FETCH d.user} avoids a lazy-load per row, and excluding
     * {@code remainingAmount <= 0} skips debts already paid off (see the class Javadoc on why
     * there is no separate {@code isActive} flag).
     *
     * <p>Relies on the {@code idx_debts_user_due_date} composite index (see
     * {@code V10__add_performance_indexes.sql}) to stay cheap as the number of debts grows.
     */
    @Query("SELECT d FROM Debt d JOIN FETCH d.user "
            + "WHERE d.remainingAmount > 0 AND d.dueDate BETWEEN :start AND :end")
    List<Debt> findWithBalanceByDueDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
