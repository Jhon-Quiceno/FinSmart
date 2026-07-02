package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.RecurringPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Persistence access for {@link RecurringPayment}, always scoped by owner.
 */
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, Long> {

    Optional<RecurringPayment> findByIdAndUser_Id(Long id, Long userId);

    Page<RecurringPayment> findAllByUser_Id(Long userId, Pageable pageable);

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
}
