package com.smartfinance.backend.tarjetas.repository;

import com.smartfinance.backend.tarjetas.model.entity.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for {@link InstallmentPlan}.
 */
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {

    /**
     * Finds the plan originated by a given {@code CardMovement} (the {@code INSTALLMENT_PURCHASE}
     * that created it) — used by {@code GET /api/cards/{id}/movements/{movementId}/installments}
     * to resolve which plan's installments to list.
     */
    Optional<InstallmentPlan> findByMovement_Id(Long movementId);
}
