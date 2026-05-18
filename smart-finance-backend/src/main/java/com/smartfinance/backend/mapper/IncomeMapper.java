package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.income.IncomeRequest;
import com.smartfinance.backend.dto.income.IncomeResponse;
import com.smartfinance.backend.model.Income;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface IncomeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Income toEntity(IncomeRequest request);

    @Mapping(target = "categoryId", expression = "java(income.getCategory() != null ? income.getCategory().getId() : null)")
    IncomeResponse toResponse(Income income);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(IncomeRequest request, @MappingTarget Income income);
}
