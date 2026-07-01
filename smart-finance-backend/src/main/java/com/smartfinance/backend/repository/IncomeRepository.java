package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Persistence access for {@link Income}, always scoped by owner.
 *
 * <p>Dynamic filters (period/source) are built via
 * {@link com.smartfinance.backend.repository.specification.IncomeSpecifications} instead of a
 * static {@code @Query}, since a JPQL {@code :param IS NULL OR ...} pattern fails against
 * PostgreSQL when the parameter is null (the driver cannot infer its type).
 */
public interface IncomeRepository extends JpaRepository<Income, Long>, JpaSpecificationExecutor<Income> {

    Optional<Income> findByIdAndUser_Id(Long id, Long userId);
}
