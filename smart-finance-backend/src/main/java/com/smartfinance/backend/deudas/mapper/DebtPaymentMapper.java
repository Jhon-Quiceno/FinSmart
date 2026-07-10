package com.smartfinance.backend.deudas.mapper;

import com.smartfinance.backend.deudas.model.dto.DebtPaymentRequest;
import com.smartfinance.backend.deudas.model.dto.DebtPaymentResponse;
import com.smartfinance.backend.deudas.model.entity.DebtPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between {@link DebtPayment} and its request/response DTOs.
 *
 * <p>{@link DebtPayment#getDebt()} and {@link DebtPayment#getPaymentDate()} are resolved and
 * assigned separately by {@code DebtPaymentService} — the debt must be validated against the
 * current user first, and the payment date defaults to today when omitted from the request.
 * {@code expenseId} on {@link DebtPaymentResponse} is also set separately by the service, since
 * it comes from the linked {@code Expense} created alongside the payment, not from the
 * {@link DebtPayment} entity itself.
 */
@Mapper(componentModel = "spring")
public interface DebtPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "debt", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    DebtPayment toEntity(DebtPaymentRequest request);

    @Mapping(target = "debtId", source = "debt.id")
    @Mapping(target = "expenseId", ignore = true)
    DebtPaymentResponse toResponse(DebtPayment debtPayment);
}
