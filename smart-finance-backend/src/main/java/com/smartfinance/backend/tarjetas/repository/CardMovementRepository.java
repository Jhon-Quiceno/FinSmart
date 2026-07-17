package com.smartfinance.backend.tarjetas.repository;

import com.smartfinance.backend.tarjetas.model.entity.CardMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link CardMovement}.
 *
 * <p>Ownership of the parent {@link com.smartfinance.backend.tarjetas.model.entity.CreditCard}
 * is verified by {@code CardMovementService} before any query here runs, so no {@code user}
 * scoping is needed at this level (mirrors {@code DebtChargeRepository}/
 * {@code DebtPaymentRepository}). The {@code type}-filtered variant is added in Fase B.2
 * alongside {@code CardMovementService} — out of scope for this slice.
 */
public interface CardMovementRepository extends JpaRepository<CardMovement, Long> {

    Page<CardMovement> findAllByCard_Id(Long cardId, Pageable pageable);
}
