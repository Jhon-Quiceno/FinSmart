package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.transaction.TransactionRequest;
import com.smartfinance.backend.dto.transaction.TransactionResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.TransactionMapper;
import com.smartfinance.backend.model.Account;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.model.Transaction;
import com.smartfinance.backend.model.TransactionType;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.AccountRepository;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.repository.TransactionRepository;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Handles CRUD operations for user transactions.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    public TransactionService(
        TransactionRepository transactionRepository,
        CategoryRepository categoryRepository,
        AccountRepository accountRepository,
        TransactionMapper transactionMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.transactionMapper = transactionMapper;
    }

    /**
     * Returns the transactions visible to the authenticated user with optional filters.
     *
     * @param type        transaction type filter
     * @param categoryId  category identifier filter
     * @param accountId   account identifier filter
     * @param fromDate    starting date filter
     * @param toDate      ending date filter
     * @param pageable    pagination parameters
     * @return paginated list of transactions
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            TransactionType type,
            Long categoryId,
            Long accountId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser mayor a la fecha final");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        return transactionRepository.findAllByFilters(userId, type, categoryId, accountId, fromDate, toDate, pageable)
                .map(transactionMapper::toResponse);
    }

    /**
     * Creates a new transaction for the authenticated user.
     *
     * @param request request payload
     * @return persisted transaction response
     */
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setUser(buildUserReference(userId));
        transaction.setCategory(resolveCategory(request.categoryId(), userId, request.type()));
        transaction.setAccount(resolveAccount(request.accountId(), userId));
        
        // Direct string assignment for simplified lookup fields
        if (request.type() == TransactionType.INCOME) {
            transaction.setIncomeSourceName(request.incomeSourceName());
        }
        if (request.type() == TransactionType.EXPENSE) {
            transaction.setExpensePaymentMethodName(request.expensePaymentMethodName());
            transaction.setExpenseTypeName(request.expenseTypeName());
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toResponse(savedTransaction);
    }

    /**
     * Updates an existing transaction owned by the authenticated user.
     *
     * @param transactionId transaction identifier
     * @param request       request payload
     * @return updated transaction response
     */
    @Transactional
    public TransactionResponse updateTransaction(Long transactionId, TransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Transaction transaction = findOwnedTransaction(transactionId, userId);

        transactionMapper.updateEntityFromRequest(request, transaction);
        transaction.setCategory(resolveCategory(request.categoryId(), userId, request.type()));
        transaction.setAccount(resolveAccount(request.accountId(), userId));
        
        // Direct string assignment for simplified lookup fields
        if (request.type() == TransactionType.INCOME) {
            transaction.setIncomeSourceName(request.incomeSourceName());
        } else {
            transaction.setIncomeSourceName(null);
        }
        if (request.type() == TransactionType.EXPENSE) {
            transaction.setExpensePaymentMethodName(request.expensePaymentMethodName());
            transaction.setExpenseTypeName(request.expenseTypeName());
        } else {
            transaction.setExpensePaymentMethodName(null);
            transaction.setExpenseTypeName(null);
        }

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toResponse(updatedTransaction);
    }

    /**
     * Deletes a transaction owned by the authenticated user.
     *
     * @param transactionId transaction identifier
     */
    @Transactional
    public void deleteTransaction(Long transactionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Transaction transaction = findOwnedTransaction(transactionId, userId);
        transactionRepository.delete(transaction);
    }

    private Transaction findOwnedTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción no encontrada"));

        if (!userId.equals(transaction.getUser().getId())) {
            throw new AccessDeniedException("No tienes permisos sobre esta transacción");
        }

        return transaction;
    }

    private Category resolveCategory(Long categoryId, Long userId, TransactionType type) {
        if (type == TransactionType.TRANSFER) {
            if (categoryId != null) {
                throw new IllegalArgumentException("Las transferencias no admiten categoría");
            }
            return null;
        }

        if (categoryId == null) {
            return null;
        }

        Category category = categoryRepository.findAccessibleByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> {
                    if (categoryRepository.existsById(categoryId)) {
                        return new AccessDeniedException("No tienes permisos sobre la categoría seleccionada");
                    }
                    return new ResourceNotFoundException("Categoría no encontrada");
                });

        CategoryType expectedType = type == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
        if (category.getType() != expectedType) {
            throw new IllegalArgumentException("La categoría seleccionada no corresponde al tipo de movimiento");
        }

        return category;
    }

    private Account resolveAccount(Long accountId, Long userId) {
        if (accountId == null) {
            return null;
        }

        return accountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> {
                    if (accountRepository.existsById(accountId)) {
                        return new AccessDeniedException("No tienes permisos sobre la cuenta seleccionada");
                    }
                    return new ResourceNotFoundException("Cuenta no encontrada");
                });
    }

    private User buildUserReference(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
