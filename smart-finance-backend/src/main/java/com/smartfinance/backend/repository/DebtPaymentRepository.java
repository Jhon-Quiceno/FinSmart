package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.DebtPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link DebtPayment}.
 *
 * <p>Ownership of the parent {@link com.smartfinance.backend.model.Debt} is verified by
 * {@code DebtPaymentService} before any query here runs, so no {@code user} scoping is
 * needed at this level.
 */
public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {

    Page<DebtPayment> findAllByDebt_IdOrderByPaymentDateDescIdDesc(Long debtId, Pageable pageable);
}
