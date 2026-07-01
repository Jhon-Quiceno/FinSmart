package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Persistence access for {@link Expense}, always scoped by owner.
 *
 * <p>Dynamic filters (category/date range/payment method) are built via
 * {@link com.smartfinance.backend.repository.specification.ExpenseSpecifications} instead of a
 * static {@code @Query}, since a JPQL {@code :param IS NULL OR ...} pattern fails against
 * PostgreSQL when the parameter is null (the driver cannot infer its type).
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByIdAndUser_Id(Long id, Long userId);
}
