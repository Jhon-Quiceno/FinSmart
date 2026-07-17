package com.smartfinance.backend.tarjetas.repository;

import com.smartfinance.backend.tarjetas.model.entity.Installment;
import com.smartfinance.backend.tarjetas.model.entity.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Persistence access for {@link Installment}.
 */
public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    /**
     * Cuotas de un plan ordenadas por número, usadas por
     * {@code GET /api/cards/{id}/movements/{movementId}/installments}.
     */
    List<Installment> findAllByPlan_IdOrderByNumber(Long planId);

    /**
     * Cuotas pendientes vencidas hasta la fecha dada, de cualquier plan de cualquier compra de la
     * tarjeta indicada. Todavía no se usa en Fase B.3 — queda lista para
     * {@code CycleCloseService} (Fase B.4), que la usa para calcular el interés agregado del
     * cierre de ciclo.
     *
     * <p>Nombre corregido respecto al design/tasks artifact: {@code InstallmentPlan} no tiene un
     * campo {@code card} directo, solo {@code movement.card} ({@link
     * com.smartfinance.backend.tarjetas.model.entity.InstallmentPlan#getMovement()}), así que la
     * derived query real necesita el salto intermedio por {@code movement}.
     */
    List<Installment> findByPlan_Movement_Card_IdAndStatusAndDueDateLessThanEqual(
            Long cardId,
            InstallmentStatus status,
            LocalDate dueDate
    );
}
