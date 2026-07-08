package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.debt.DebtRequest;
import com.smartfinance.backend.dto.debt.DebtResponse;
import com.smartfinance.backend.dto.debt.DebtUpdateRequest;
import com.smartfinance.backend.model.Debt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper between {@link Debt} and its request/response DTOs.
 *
 * <p>{@link Debt#getUser()} is resolved and assigned separately by {@code DebtService}.
 * {@link Debt#getRemainingAmount()} is seeded from {@code totalAmount} by {@code DebtService}
 * on creation and is never touched by {@link #updateEntityFromRequest}, which maps from
 * {@link DebtUpdateRequest} — a DTO that intentionally has no total/remaining fields (see its
 * Javadoc for the traceability rationale).
 */
@Mapper(componentModel = "spring")
public interface DebtMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "remainingAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Debt toEntity(DebtRequest request);

    DebtResponse toResponse(Debt debt);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "remainingAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(DebtUpdateRequest request, @MappingTarget Debt debt);
}
