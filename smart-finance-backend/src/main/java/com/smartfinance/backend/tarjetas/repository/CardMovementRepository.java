package com.smartfinance.backend.tarjetas.repository;

import com.smartfinance.backend.tarjetas.model.entity.CardMovement;
import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a persistencia de {@link CardMovement}.
 *
 * <p>La pertenencia de la {@link com.smartfinance.backend.tarjetas.model.entity.CreditCard}
 * padre la valida {@code CardMovementService} antes de ejecutar cualquier consulta acá, por lo
 * que no se necesita un filtro adicional por usuario en este nivel (mirror de
 * {@code DebtChargeRepository}/{@code DebtPaymentRepository}).
 */
public interface CardMovementRepository extends JpaRepository<CardMovement, Long> {

    Page<CardMovement> findAllByCard_Id(Long cardId, Pageable pageable);

    /**
     * Variante filtrada por {@link CardMovementType}, usada por
     * {@code GET /api/cards/{id}/movements?type=} cuando el cliente pide un tipo de movimiento
     * específico.
     */
    Page<CardMovement> findAllByCard_IdAndType(Long cardId, CardMovementType type, Pageable pageable);
}
