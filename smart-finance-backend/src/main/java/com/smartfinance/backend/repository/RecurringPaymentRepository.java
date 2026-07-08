package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.RecurringPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link RecurringPayment}, always scoped by owner.
 */
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, Long> {

    Optional<RecurringPayment> findByIdAndUser_Id(Long id, Long userId);

    Page<RecurringPayment> findAllByUser_Id(Long userId, Pageable pageable);

    /**
     * Used by {@code com.smartfinance.backend.service.ai.FinancialContextBuilder} to list the
     * active recurring payments included in the AI system prompt.
     */
    List<RecurringPayment> findAllByUser_IdAndActiveTrue(Long userId);

    /**
     * Atomically advances {@code nextPaymentDate} to {@code newDate}, but only if it still
     * equals {@code currentDate} — guarding against a duplicate {@code PATCH /pay} execution
     * (client retry after timeout, double-click, two open tabs) where two concurrent calls both
     * read the same {@code nextPaymentDate}, both compute the same {@code newDate}, and both
     * would otherwise create a duplicate {@link com.smartfinance.backend.model.Expense}.
     *
     * @return {@code 1} if this call won the race and advanced the date, {@code 0} if another
     *         execution already advanced it first — callers must not create the Expense in
     *         that case.
     */
    @Modifying
    @Query("UPDATE RecurringPayment r SET r.nextPaymentDate = :newDate "
            + "WHERE r.id = :id AND r.nextPaymentDate = :currentDate")
    int advanceNextPaymentDate(
            @Param("id") Long id,
            @Param("currentDate") LocalDate currentDate,
            @Param("newDate") LocalDate newDate
    );

    /**
     * Scans every active recurring payment (across all users) whose {@code nextPaymentDate}
     * falls within {@code [start, end]}, backing {@code PaymentReminderJob} (Sprint 5, Batch 3).
     * {@code JOIN FETCH r.user} avoids a lazy-load per row when the job reads
     * {@code payment.getUser().getId()} to dispatch the reminder.
     *
     * <p>A single scan across all users, rather than one query per user, is what keeps this job
     * cheap regardless of how many users the platform has — it relies on the
     * {@code idx_recurring_payments_active_next_date} composite index (see
     * {@code V10__add_performance_indexes.sql}).
     */
    @Query("SELECT r FROM RecurringPayment r JOIN FETCH r.user "
            + "WHERE r.active = true AND r.nextPaymentDate BETWEEN :start AND :end")
    List<RecurringPayment> findActiveByNextPaymentDateBetween(
            @Param("start") LocalDate start, @Param("end") LocalDate end
    );
}
