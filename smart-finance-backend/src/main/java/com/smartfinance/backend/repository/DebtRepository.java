package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Debt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for {@link Debt}, always scoped by owner.
 */
public interface DebtRepository extends JpaRepository<Debt, Long> {

    Optional<Debt> findByIdAndUser_Id(Long id, Long userId);

    Page<Debt> findAllByUser_Id(Long userId, Pageable pageable);
}
