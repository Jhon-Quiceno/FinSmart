package com.smartfinance.backend.deudas.repository;

import com.smartfinance.backend.deudas.model.entity.DebtCharge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link DebtCharge}.
 *
 * <p>Ownership of the parent {@link com.smartfinance.backend.deudas.model.entity.Debt} is verified by
 * {@code DebtChargeService} before any query here runs, so no {@code user} scoping is
 * needed at this level.
 */
public interface DebtChargeRepository extends JpaRepository<DebtCharge, Long> {

    Page<DebtCharge> findAllByDebt_IdOrderByChargeDateDescIdDesc(Long debtId, Pageable pageable);
}
