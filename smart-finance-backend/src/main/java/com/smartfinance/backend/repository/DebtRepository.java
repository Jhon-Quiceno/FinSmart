package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Debt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Persistence access for {@link Debt}, always scoped by owner.
 */
public interface DebtRepository extends JpaRepository<Debt, Long> {

    Optional<Debt> findByIdAndUser_Id(Long id, Long userId);

    Page<Debt> findAllByUser_Id(Long userId, Pageable pageable);

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
     *         payment and must not persist a {@link com.smartfinance.backend.model.DebtPayment}.
     */
    @Modifying
    @Query("UPDATE Debt d SET d.remainingAmount = d.remainingAmount - :amount "
            + "WHERE d.id = :id AND d.remainingAmount >= :amount")
    int decrementRemainingAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
