package com.smartfinance.backend.tarjetas.mapper;

import com.smartfinance.backend.tarjetas.model.dto.InstallmentResponse;
import com.smartfinance.backend.tarjetas.model.entity.Installment;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper entre {@link Installment} y {@link InstallmentResponse}. Todos los campos de
 * ambos lados coinciden por nombre, por lo que no se necesita ningún {@code @Mapping} explícito.
 */
@Mapper(componentModel = "spring")
public interface InstallmentMapper {

    InstallmentResponse toResponse(Installment installment);
}
