package com.smartfinance.backend.repository.specification;

import com.smartfinance.backend.model.Expense;
import com.smartfinance.backend.model.PaymentMethodType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Dynamic {@link Specification} builders for {@link Expense} filters.
 *
 * <p>Each method returns {@code null} from its predicate when the filter value is absent,
 * which {@link Specification#and(Specification)} treats as "no restriction". This avoids
 * binding null parameters into JPQL comparisons directly (e.g. {@code :paymentMethod IS NULL
 * OR e.paymentMethod = :paymentMethod}), which fails against PostgreSQL because the driver
 * cannot infer the parameter's type when it is null (see the checksum errors this replaced:
 * "could not determine data type of parameter").
 */
public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<Expense> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Expense> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null
                ? null
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Expense> dateFrom(LocalDate from) {
        return (root, query, cb) -> from == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("date"), from);
    }

    public static Specification<Expense> dateTo(LocalDate to) {
        return (root, query, cb) -> to == null
                ? null
                : cb.lessThanOrEqualTo(root.get("date"), to);
    }

    public static Specification<Expense> hasPaymentMethod(PaymentMethodType paymentMethod) {
        return (root, query, cb) -> paymentMethod == null
                ? null
                : cb.equal(root.get("paymentMethod"), paymentMethod);
    }
}
