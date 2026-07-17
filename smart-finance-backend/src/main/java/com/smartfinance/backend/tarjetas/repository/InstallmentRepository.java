package com.smartfinance.backend.tarjetas.repository;

import com.smartfinance.backend.tarjetas.model.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link Installment}.
 *
 * <p>Bare {@link JpaRepository} for this slice — {@code findByPlan_Card_IdAndStatusAndDueDateLessThanEqual}
 * (used by the cycle-close job) is added in Fase B.4 alongside {@code CycleCloseService}.
 */
public interface InstallmentRepository extends JpaRepository<Installment, Long> {
}
