package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findAllByUser_Id(Long userId, Pageable pageable);

    Page<Expense> findByUser_IdAndDateBetween(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    Page<Expense> findByUser_IdAndCategory_Id(Long userId, Long categoryId, Pageable pageable);

    @Query("""
            SELECT e
            FROM Expense e
            WHERE e.user.id = :userId
              AND (:categoryId IS NULL OR e.category.id = :categoryId)
              AND (:fromDate IS NULL OR e.date >= :fromDate)
              AND (:toDate IS NULL OR e.date <= :toDate)
            """)
    Page<Expense> findAllByFilters(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );
}
