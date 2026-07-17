package com.smartfinance.backend.tarjetas.service;

import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.servicios.service.notification.NotificationDispatcher;
import com.smartfinance.backend.tarjetas.model.entity.CardMovement;
import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.model.entity.Installment;
import com.smartfinance.backend.tarjetas.model.entity.InstallmentStatus;
import com.smartfinance.backend.tarjetas.repository.CardMovementRepository;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
import com.smartfinance.backend.tarjetas.repository.InstallmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Materializa, por tarjeta, el interés de las cuotas {@link InstallmentStatus#PENDING} que ya
 * vencieron al cierre de ciclo (Fase B.4, design decision 3).
 *
 * <p>{@link #closeCycle(Long)} es el único punto de entrada, pensado para ser invocado por
 * {@code CardCycleCloseJob} una vez por tarjeta. Es idempotente: un guard atómico
 * ({@link CreditCardRepository#markCutoffClosed}) evita duplicar el interés si el job corre dos
 * veces el mismo día, y la MISMA consulta que resuelve las cuotas vencidas
 * ({@link InstallmentRepository#findByPlan_Movement_Card_IdAndStatusAndDueDateLessThanEqual})
 * también resuelve el "catch-up": si el job estuvo caído el día exacto del corte, la próxima
 * corrida sigue viendo esas cuotas como {@code PENDING} con {@code dueDate} en el pasado y las
 * factura igual, sin perder el ciclo (cierra el warning de catch-up del design-review).
 */
@Service
public class CycleCloseService {

    private static final Logger log = LoggerFactory.getLogger(CycleCloseService.class);
    private static final String CARD_NOT_FOUND_MESSAGE = "Tarjeta no encontrada";

    private final CreditCardRepository creditCardRepository;
    private final InstallmentRepository installmentRepository;
    private final CardMovementRepository cardMovementRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final Clock clock;

    public CycleCloseService(
            CreditCardRepository creditCardRepository,
            InstallmentRepository installmentRepository,
            CardMovementRepository cardMovementRepository,
            NotificationDispatcher notificationDispatcher,
            Clock clock
    ) {
        this.creditCardRepository = creditCardRepository;
        this.installmentRepository = installmentRepository;
        this.cardMovementRepository = cardMovementRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.clock = clock;
    }

    /**
     * Cierra el ciclo de facturación de la tarjeta {@code cardId} a la fecha de hoy (según
     * {@link #clock}):
     * <ol>
     *     <li>Guard atómico de idempotencia/catch-up ({@link CreditCardRepository#markCutoffClosed}):
     *     si devuelve {@code 0} filas, el ciclo ya fue cerrado hoy (o después) y el método
     *     retorna sin hacer nada más.</li>
     *     <li>Busca las cuotas {@code PENDING} de la tarjeta con {@code dueDate <= hoy}.</li>
     *     <li>Si hay alguna, suma sus {@code interestAmount}, crea UN {@link CardMovement}
     *     agregado tipo {@link CardMovementType#INTEREST} por el total y lo suma al saldo vía
     *     {@link CreditCardRepository#incrementBalance} (sin guard de cupo: el interés siempre
     *     se aplica).</li>
     *     <li>Marca esas cuotas como {@link InstallmentStatus#BILLED} y les asigna el movimiento
     *     de interés recién creado.</li>
     *     <li>Notifica al dueño de la tarjeta (dedupe key incluye {@code cardId} y la fecha de
     *     cierre, mirror de {@code PaymentReminderJob}).</li>
     * </ol>
     *
     * <p>Si el guard pasa pero no hay cuotas vencidas (tarjeta sin compras a cuotas, o ninguna
     * vencida todavía), no se crea movimiento de interés ni se notifica — solo queda registrado
     * que el ciclo se revisó hoy.
     */
    @Transactional
    public void closeCycle(Long cardId) {
        LocalDate today = LocalDate.now(clock);

        int guardedRows = creditCardRepository.markCutoffClosed(cardId, today);
        if (guardedRows == 0) {
            log.debug("cycle_close_skipped_already_closed cardId={} today={}", cardId, today);
            return;
        }

        List<Installment> dueInstallments = installmentRepository
                .findByPlan_Movement_Card_IdAndStatusAndDueDateLessThanEqual(cardId, InstallmentStatus.PENDING, today);

        if (dueInstallments.isEmpty()) {
            log.debug("cycle_close_no_pending_installments cardId={} today={}", cardId, today);
            return;
        }

        BigDecimal interestTotal = dueInstallments.stream()
                .map(Installment::getInterestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CreditCard card = creditCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_MESSAGE));

        CardMovement interestMovement = new CardMovement();
        interestMovement.setCard(card);
        interestMovement.setType(CardMovementType.INTEREST);
        interestMovement.setAmount(interestTotal);
        interestMovement.setDate(today);
        interestMovement.setDescription("Interés del cierre de ciclo");
        interestMovement.setCycleCloseDate(today);
        CardMovement savedInterestMovement = cardMovementRepository.save(interestMovement);

        creditCardRepository.incrementBalance(cardId, interestTotal);

        dueInstallments.forEach(installment -> {
            installment.setStatus(InstallmentStatus.BILLED);
            installment.setInterestMovement(savedInterestMovement);
        });
        installmentRepository.saveAll(dueInstallments);

        CreditCard cardAfter = creditCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_MESSAGE));

        dispatchCycleCloseNotification(cardAfter, today);
    }

    private void dispatchCycleCloseNotification(CreditCard card, LocalDate closeDate) {
        String title = "Cierre de ciclo: " + card.getName();
        String message = "Tu tarjeta " + card.getName() + " cerró su ciclo, saldo actual $"
                + formatAmount(card.getCurrentBalance()) + ".";
        String dedupeKey = "card-cycle-close:" + card.getId() + ":" + closeDate;

        notificationDispatcher.dispatch(
                card.getUser().getId(), NotificationType.CARD_CYCLE_CLOSE, title, message, dedupeKey
        );
    }

    /** Formatea {@code amount} como cifra entera con separador de miles "." (ej. {@code "24.500"}). */
    private static String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal rounded = safeAmount.setScale(0, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(rounded);
    }
}
