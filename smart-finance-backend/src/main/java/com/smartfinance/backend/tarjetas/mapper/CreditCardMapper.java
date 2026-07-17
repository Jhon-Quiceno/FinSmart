package com.smartfinance.backend.tarjetas.mapper;

import com.smartfinance.backend.tarjetas.model.dto.CreditCardRequest;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardResponse;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardUpdateRequest;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper between {@link CreditCard} and its request/response DTOs.
 *
 * <p>{@link CreditCard#getUser()} is resolved and assigned separately by {@code CreditCardService}.
 * {@link CreditCard#getCurrentBalance()} is seeded to zero by {@code CreditCardService} on
 * creation and is never touched by {@link #updateEntityFromRequest}, which maps from
 * {@link CreditCardUpdateRequest} — a DTO that intentionally has no franchise/creditLimit/balance
 * fields (see its Javadoc for the traceability rationale). {@link #toResponse} derives
 * {@code availableCredit} at mapping time; it is never a stored column.
 */
@Mapper(componentModel = "spring")
public interface CreditCardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "lastCutoffDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CreditCard toEntity(CreditCardRequest request);

    @Mapping(target = "availableCredit", expression = "java(card.getCreditLimit().subtract(card.getCurrentBalance()))")
    CreditCardResponse toResponse(CreditCard card);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "franchise", ignore = true)
    @Mapping(target = "creditLimit", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "lastCutoffDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CreditCardUpdateRequest request, @MappingTarget CreditCard card);
}
