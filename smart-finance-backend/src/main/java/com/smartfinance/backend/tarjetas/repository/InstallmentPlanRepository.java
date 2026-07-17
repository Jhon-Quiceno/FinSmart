package com.smartfinance.backend.tarjetas.repository;

import com.smartfinance.backend.tarjetas.model.entity.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link InstallmentPlan}.
 *
 * <p>Bare {@link JpaRepository} for this slice — {@code findByMovement_Id} and the amortization
 * write path are added in Fase B.3 alongside {@code AmortizationService}.
 */
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {
}
