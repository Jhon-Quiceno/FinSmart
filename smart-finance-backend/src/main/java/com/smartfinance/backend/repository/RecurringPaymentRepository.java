package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.RecurringPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for {@link RecurringPayment}, always scoped by owner.
 */
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, Long> {

    Optional<RecurringPayment> findByIdAndUser_Id(Long id, Long userId);

    Page<RecurringPayment> findAllByUser_Id(Long userId, Pageable pageable);
}
