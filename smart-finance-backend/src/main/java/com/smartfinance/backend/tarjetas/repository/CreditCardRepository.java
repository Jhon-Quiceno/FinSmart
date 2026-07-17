package com.smartfinance.backend.tarjetas.repository;

import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
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
 * Acceso a persistencia de {@link CreditCard}, siempre delimitado por el dueño.
 */
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    Optional<CreditCard> findByIdAndUser_Id(Long id, Long userId);

    Page<CreditCard> findAllByUser_Id(Long userId, Pageable pageable);

    /**
     * Incrementa {@code currentBalance} de forma atómica, únicamente si el nuevo saldo no supera
     * el cupo de la tarjeta ({@code creditLimit}). El {@code WHERE} convierte el incremento y la
     * validación de cupo en una sola operación de base de datos (no lectura-y-luego-escritura),
     * cerrando la misma carrera de "lost update" que {@code DebtRepository#decrementRemainingAmount}
     * cierra para deudas.
     *
     * <p>{@code clearAutomatically = true} desvincula el contexto de persistencia después del
     * {@code UPDATE} masivo, para que la relectura posterior de la tarjeta (necesaria para
     * calcular {@code cardBalanceAfter} en la respuesta) vaya a la base de datos en vez de
     * devolver la entidad obsoleta del caché de primer nivel.
     *
     * @return {@code 1} si el incremento se aplicó, {@code 0} si {@code amount} haría que el
     *         saldo superara el cupo disponible — quien llama debe tratar {@code 0} como una
     *         compra rechazada y no debe persistir el {@link com.smartfinance.backend.tarjetas.model.entity.CardMovement}.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditCard c SET c.currentBalance = c.currentBalance + :amount "
            + "WHERE c.id = :id AND c.currentBalance + :amount <= c.creditLimit")
    int incrementBalanceWithinLimit(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * Incrementa {@code currentBalance} de forma atómica, sin validar el cupo. Pensado para
     * movimientos generados por el sistema que siempre deben aplicarse — usado por
     * {@code CycleCloseService} (Fase B.4) para materializar el interés agregado del cierre de
     * ciclo, que nunca debe rechazarse por cupo.
     *
     * @return {@code 1} si el incremento se aplicó, {@code 0} si la tarjeta ya no existe (por
     *         ejemplo, fue eliminada de forma concurrente)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditCard c SET c.currentBalance = c.currentBalance + :amount WHERE c.id = :id")
    int incrementBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * Decrementa {@code currentBalance} de forma atómica, únicamente si el saldo actual alcanza
     * para cubrir {@code amount}. Mirror de {@code DebtRepository#decrementRemainingAmount},
     * usado por {@code CardMovementService#registerPayment}.
     *
     * @return {@code 1} si el decremento se aplicó, {@code 0} si {@code amount} supera el saldo
     *         actual de la tarjeta — quien llama debe tratar {@code 0} como un pago rechazado y
     *         no debe persistir el {@link com.smartfinance.backend.tarjetas.model.entity.CardMovement}.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditCard c SET c.currentBalance = c.currentBalance - :amount "
            + "WHERE c.id = :id AND c.currentBalance >= :amount")
    int decrementBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * Suma {@code currentBalance} de todas las tarjetas del usuario, usado por
     * {@code FinancialAnalysisService} para sumar el saldo de tarjetas al total de deuda del
     * cálculo de {@code debtRatio}, junto a {@code DebtRepository#sumRemainingAmountByUser}.
     */
    @Query("SELECT COALESCE(SUM(c.currentBalance), 0) FROM CreditCard c WHERE c.user.id = :userId")
    BigDecimal sumCurrentBalanceByUser(@Param("userId") Long userId);

    /**
     * Guard atómico de idempotencia para el cierre de ciclo (Fase B.4): marca la tarjeta como
     * cerrada hasta {@code closeDate} únicamente si todavía no lo estaba. Corre dentro de la
     * misma transacción en la que {@code CycleCloseService#closeCycle} crea el movimiento
     * {@code INTEREST}, así que si el job corre dos veces el mismo día (o dos instancias
     * concurrentes procesan la misma tarjeta), la segunda ejecución ve {@code 0} filas
     * actualizadas y no debe tocar nada más — evita duplicar el interés materializado.
     *
     * @return {@code 1} si el guard se aplicó (la tarjeta queda marcada como cerrada hasta
     *         {@code closeDate}), {@code 0} si ya estaba cerrada para esa fecha o una posterior
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditCard c SET c.lastCutoffDate = :closeDate "
            + "WHERE c.id = :id AND (c.lastCutoffDate IS NULL OR c.lastCutoffDate < :closeDate)")
    int markCutoffClosed(@Param("id") Long id, @Param("closeDate") LocalDate closeDate);

    /**
     * Candidatas a cierre de ciclo para {@code CardCycleCloseJob} (Fase B.4): toda tarjeta que
     * nunca cerró un ciclo o cuyo último cierre es anterior a {@code today}. Consulta amplia y
     * cross-user (mirror de {@code DebtRepository#findWithBalanceByDueDateBetween}) que
     * deliberadamente NO filtra por "{@code cutoffDay} == día de hoy": eso perdería el ciclo
     * completo si el servidor estuvo caído justo el día del corte (cierra el warning de
     * catch-up del design-review). El filtro fino —¿el corte correspondiente a esta tarjeta ya
     * venció?— lo hace el job en Java por tarjeta, porque {@code cutoffDay} es un día del mes
     * (1-31), no una fecha, y resolverlo requiere el mes/año efectivos de "hoy".
     */
    @Query("SELECT c FROM CreditCard c WHERE c.lastCutoffDate IS NULL OR c.lastCutoffDate < :today")
    List<CreditCard> findCardsPendingCycleClose(@Param("today") LocalDate today);
}
