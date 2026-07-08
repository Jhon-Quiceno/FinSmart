package com.smartfinance.backend.repository.specification;

import com.smartfinance.backend.model.Income;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Dynamic {@link Specification} builders for {@link Income} filters.
 *
 * <p>Each method returns {@code null} from its predicate when the filter value is absent,
 * which {@link Specification#and(Specification)} treats as "no restriction". This avoids
 * binding null parameters into JPQL comparisons directly, which fails against PostgreSQL
 * because the driver cannot infer the parameter's type when it is null — this previously broke
 * {@code source} filtering (wrapping a null parameter in {@code LOWER(...)} made PostgreSQL
 * infer it as {@code bytea}) and the {@code EXTRACT(MONTH/YEAR FROM ...)} filter.
 *
 * <p>{@link #inPeriod} replaces the original {@code EXTRACT(MONTH FROM date) = :month} filter
 * with a plain date-range comparison, which is both null-safe and able to use the {@code date}
 * index created in {@code V3__create_categories_incomes_expenses.sql} (EXTRACT() cannot). If
 * only {@code month} is given without {@code year}, the current year is assumed.
 */
public final class IncomeSpecifications {

    private IncomeSpecifications() {
    }

    public static Specification<Income> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Income> inPeriod(Integer month, Integer year) {
        return (root, query, cb) -> {
            if (month == null && year == null) {
                return null;
            }

            int resolvedYear = year != null ? year : LocalDate.now().getYear();
            LocalDate start;
            LocalDate end;
            if (month != null) {
                start = LocalDate.of(resolvedYear, month, 1);
                end = start.plusMonths(1);
            } else {
                start = LocalDate.of(resolvedYear, 1, 1);
                end = start.plusYears(1);
            }

            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("date"), start),
                    cb.lessThan(root.get("date"), end)
            );
        };
    }

    public static Specification<Income> hasSource(String source) {
        return (root, query, cb) -> source == null || source.isBlank()
                ? null
                : cb.equal(cb.lower(root.get("source")), source.toLowerCase());
    }
}
