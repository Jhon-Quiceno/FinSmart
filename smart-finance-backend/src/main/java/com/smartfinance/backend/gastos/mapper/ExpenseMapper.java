package com.smartfinance.backend.gastos.mapper;

import com.smartfinance.backend.gastos.model.dto.ExpenseRequest;
import com.smartfinance.backend.gastos.model.dto.ExpenseResponse;
import com.smartfinance.backend.gastos.model.entity.Expense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper between {@link Expense} and its request/response DTOs.
 *
 * <p>{@link Expense#getCategory()} is resolved and assigned separately by
 * {@code ExpenseService} (it must be validated against the current user before being set),
 * so both {@link #toEntity(ExpenseRequest)} and
 * {@link #updateEntityFromRequest(ExpenseRequest, Expense)} ignore it.
 * {@link Expense#getRecurringPayment()} has no counterpart in {@link ExpenseRequest} — it is
 * only ever set by {@code RecurringPaymentService#payRecurringPayment}, never through the
 * regular expense create/update endpoints — so both methods ignore it too.
 */
@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "recurringPayment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Expense toEntity(ExpenseRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "recurringPaymentId", source = "recurringPayment.id")
    ExpenseResponse toResponse(Expense expense);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "recurringPayment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ExpenseRequest request, @MappingTarget Expense expense);
}
