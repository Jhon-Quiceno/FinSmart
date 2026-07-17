package com.smartfinance.backend.tarjetas.service.job;

import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
import com.smartfinance.backend.tarjetas.service.CycleCloseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Daily job that closes the billing cycle of every {@link CreditCard} whose cutoff day has
 * arrived, delegating the actual interest materialization to
 * {@link CycleCloseService#closeCycle(Long)} — mirrors
 * {@code com.smartfinance.backend.servicios.service.job.PaymentReminderJob}'s structure (cross-user
 * scan, injected {@link Clock}, not itself {@code @Transactional}).
 *
 * <p>Unlike a naive "{@code cutoffDay == today}" scan, {@link CreditCardRepository#findCardsPendingCycleClose}
 * returns every card not yet closed today (broad candidate set), and this job then decides, per
 * card, whether its cutoff has actually arrived via {@link #isCutoffDue}. This two-step design is
 * what makes the job resilient to downtime: if the server was down exactly on a card's cutoff
 * day, the next run still sees that card as a candidate (its {@code lastCutoffDate} is still
 * stale) and still finds its cutoff day "due" (already passed this month), so the missed cycle is
 * closed instead of being lost forever — closing the design-review catch-up warning.
 *
 * <p>Each card is closed independently inside its own try/catch: an exception closing one card
 * (e.g. a data issue specific to that card) is logged and does not abort the rest of the batch.
 */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CardCycleCloseJob {

    private static final Logger log = LoggerFactory.getLogger(CardCycleCloseJob.class);

    private final CreditCardRepository creditCardRepository;
    private final CycleCloseService cycleCloseService;
    private final Clock clock;

    public CardCycleCloseJob(
            CreditCardRepository creditCardRepository,
            CycleCloseService cycleCloseService,
            Clock clock
    ) {
        this.creditCardRepository = creditCardRepository;
        this.cycleCloseService = cycleCloseService;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.jobs.card-cycle-close.cron:0 0 6 * * *}")
    public void closeDueCycles() {
        LocalDate today = LocalDate.now(clock);

        var candidates = creditCardRepository.findCardsPendingCycleClose(today);
        log.debug("card_cycle_close_job_scan candidates={} today={}", candidates.size(), today);

        candidates.stream()
                .filter(card -> isCutoffDue(card, today))
                .forEach(card -> closeCardSafely(card.getId(), today));
    }

    /**
     * A card's cutoff is "due" when the most recent cutoff date that is not after {@code today}
     * (this month's, or last month's if this month's has not arrived yet) is still newer than
     * the card's {@code lastCutoffDate} — i.e. that specific cycle was never closed.
     */
    private boolean isCutoffDue(CreditCard card, LocalDate today) {
        LocalDate effectiveCutoffDate = effectiveCutoffDate(card.getCutoffDay(), today);
        return card.getLastCutoffDate() == null || card.getLastCutoffDate().isBefore(effectiveCutoffDate);
    }

    /**
     * Resolves the most recent cutoff date (clamped to the last day of a shorter month, spec
     * "Cierre de ciclo en meses de distinta duración") that is on or before {@code today}: this
     * month's cutoff if it has already arrived, otherwise last month's.
     */
    private LocalDate effectiveCutoffDate(int cutoffDay, LocalDate today) {
        LocalDate thisMonthCutoff = clampToMonth(YearMonth.from(today), cutoffDay);
        if (!thisMonthCutoff.isAfter(today)) {
            return thisMonthCutoff;
        }
        return clampToMonth(YearMonth.from(today).minusMonths(1), cutoffDay);
    }

    private LocalDate clampToMonth(YearMonth month, int day) {
        int clampedDay = Math.min(day, month.lengthOfMonth());
        return month.atDay(clampedDay);
    }

    private void closeCardSafely(Long cardId, LocalDate today) {
        try {
            cycleCloseService.closeCycle(cardId);
        } catch (RuntimeException ex) {
            log.error("card_cycle_close_job_failed cardId={} today={}", cardId, today, ex);
        }
    }
}
