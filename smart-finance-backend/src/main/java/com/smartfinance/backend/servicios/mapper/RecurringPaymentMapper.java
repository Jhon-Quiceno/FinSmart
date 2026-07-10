package com.smartfinance.backend.servicios.mapper;

import com.smartfinance.backend.servicios.model.dto.RecurringPaymentRequest;
import com.smartfinance.backend.servicios.model.dto.RecurringPaymentResponse;
import com.smartfinance.backend.servicios.model.dto.RecurringPaymentUpdateRequest;
import com.smartfinance.backend.servicios.model.entity.RecurringPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper between {@link RecurringPayment} and its request/response DTOs.
 *
 * <p>{@link RecurringPayment#getUser()}, {@link RecurringPayment#getNextPaymentDate()} and
 * {@link RecurringPayment#isActive()} are all resolved and assigned separately by
 * {@code RecurringPaymentService}: the owner is resolved from the security context, the next
 * payment date is seeded from {@code firstPaymentDate} (and later only advanced by the
 * {@code /pay} endpoint), and the active flag only flips through the {@code /toggle} endpoint.
 */
@Mapper(componentModel = "spring")
public interface RecurringPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "nextPaymentDate", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RecurringPayment toEntity(RecurringPaymentRequest request);

    @Mapping(target = "isActive", source = "active")
    RecurringPaymentResponse toResponse(RecurringPayment recurringPayment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "nextPaymentDate", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(RecurringPaymentUpdateRequest request, @MappingTarget RecurringPayment recurringPayment);
}
