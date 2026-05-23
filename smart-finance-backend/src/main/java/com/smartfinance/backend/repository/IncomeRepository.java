package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Income;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    Page<Income> findAllByUser_Id(Long userId, Pageable pageable);

    Page<Income> findByUser_IdAndDateBetween(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    Page<Income> findByUser_IdAndCategory_Id(Long userId, Long categoryId, Pageable pageable);

    @Query("""
        SELECT i
        FROM Income i
        WHERE i.user.id = :userId
        AND (COALESCE(:categoryId, 0) = 0 OR i.category.id = :categoryId)
        AND (COALESCE(:fromDate, CAST('0001-01-01' AS date)) = CAST('0001-01-01' AS date) OR i.date >= :fromDate)
        AND (COALESCE(:toDate, CAST('9999-12-31' AS date)) = CAST('9999-12-31' AS date) OR i.date <= :toDate)
        """)
    Page<Income> findAllByFilters(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );
}
