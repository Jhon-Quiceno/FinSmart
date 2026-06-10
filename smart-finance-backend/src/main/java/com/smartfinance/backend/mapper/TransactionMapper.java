package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.transaction.TransactionRequest;
import com.smartfinance.backend.dto.transaction.TransactionResponse;
import com.smartfinance.backend.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(TransactionRequest request);

    @Mapping(target = "accountId", expression = "java(transaction.getAccount() != null ? transaction.getAccount().getId() : null)")
    @Mapping(target = "accountName", expression = "java(transaction.getAccount() != null ? transaction.getAccount().getName() : null)")
    @Mapping(target = "categoryId", expression = "java(transaction.getCategory() != null ? transaction.getCategory().getId() : null)")
    @Mapping(target = "categoryName", expression = "java(transaction.getCategory() != null ? transaction.getCategory().getName() : null)")
    @Mapping(target = "incomeSourceName", source = "incomeSourceName")
    @Mapping(target = "expensePaymentMethodName", source = "expensePaymentMethodName")
    @Mapping(target = "expenseTypeName", source = "expenseTypeName")
    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(TransactionRequest request, @MappingTarget Transaction transaction);
}
