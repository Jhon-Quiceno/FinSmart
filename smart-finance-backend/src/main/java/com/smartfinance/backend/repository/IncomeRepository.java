package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Income;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link Income}, always scoped by owner.
 *
 * <p>Dynamic filters (period/source) are built via
 * {@link com.smartfinance.backend.repository.specification.IncomeSpecifications} instead of a
 * static {@code @Query}, since a JPQL {@code :param IS NULL OR ...} pattern fails against
 * PostgreSQL when the parameter is null (the driver cannot infer its type).
 *
 * <p>{@link #sumAmountByUserAndPeriod} and {@link #findRecentByUserId} back
 * {@code FinancialAnalysisService} (Sprint 4); their filters (userId + a fixed date range) are
 * never optional, so a plain {@code @Query}/derived method is used instead of a
 * {@link org.springframework.data.jpa.domain.Specification} builder.
 */
public interface IncomeRepository extends JpaRepository<Income, Long>, JpaSpecificationExecutor<Income> {

    Optional<Income> findByIdAndUser_Id(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i "
            + "WHERE i.user.id = :userId AND i.date >= :start AND i.date <= :end")
    BigDecimal sumAmountByUserAndPeriod(
            @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end
    );

    /**
     * {@code LEFT JOIN FETCH category} so rendering {@code category.name} for each of the (at
     * most 10) rows returned doesn't trigger a lazy-load per row — without it, building the
     * "recent transactions" list is an N+1 (one extra SELECT per row) on every
     * {@code GET /api/analysis/summary} call.
     */
    @Query("SELECT i FROM Income i LEFT JOIN FETCH i.category WHERE i.user.id = :userId "
            + "ORDER BY i.date DESC, i.id DESC")
    List<Income> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
