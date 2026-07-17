package com.smartfinance.backend.tarjetas.repository;

import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for {@link CreditCard}, always scoped by owner.
 *
 * <p>Atomic balance updates ({@code incrementBalanceWithinLimit}/{@code incrementBalance}/
 * {@code decrementBalance}) and the {@code sumCurrentBalanceByUser} debtRatio aggregation are
 * added in Fase B.2, mirroring {@code DebtRepository}'s equivalent methods — out of scope for
 * this slice (entities + migrations only).
 */
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    Optional<CreditCard> findByIdAndUser_Id(Long id, Long userId);

    Page<CreditCard> findAllByUser_Id(Long userId, Pageable pageable);
}
