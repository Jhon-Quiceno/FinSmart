package com.smartfinance.backend.deudas.mapper;

import com.smartfinance.backend.deudas.model.dto.DebtChargeRequest;
import com.smartfinance.backend.deudas.model.dto.DebtChargeResponse;
import com.smartfinance.backend.deudas.model.entity.DebtCharge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between {@link DebtCharge} and its request/response DTOs.
 *
 * <p>{@link DebtCharge#getDebt()} and {@link DebtCharge#getChargeDate()} are resolved and
 * assigned separately by {@code DebtChargeService} — the debt must be validated against the
 * current user first, and the charge date defaults to today when omitted from the request.
 */
@Mapper(componentModel = "spring")
public interface DebtChargeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "debt", ignore = true)
    @Mapping(target = "chargeDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    DebtCharge toEntity(DebtChargeRequest request);

    @Mapping(target = "debtId", source = "debt.id")
    DebtChargeResponse toResponse(DebtCharge debtCharge);
}
