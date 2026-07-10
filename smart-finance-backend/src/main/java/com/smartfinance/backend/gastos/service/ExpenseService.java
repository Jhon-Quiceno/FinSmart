package com.smartfinance.backend.gastos.service;

import com.smartfinance.backend.gastos.model.dto.ExpenseRequest;
import com.smartfinance.backend.gastos.model.dto.ExpenseResponse;
import com.smartfinance.backend.gastos.event.ExpenseCreatedEvent;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.gastos.mapper.ExpenseMapper;
import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.CategoryRepository;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.gastos.repository.specification.ExpenseSpecifications;
import com.smartfinance.backend.common.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Business logic for managing the current user's {@link Expense} records.
 *
 * <p>Every operation resolves the caller via {@link SecurityUtils#getCurrentUserId()} and
 * scopes reads/writes strictly to that user. Mutations on an expense owned by another user,
 * or a {@code categoryId} referencing a category owned by another user, raise
 * {@link ResourceNotFoundException} (HTTP 404) rather than a 403, so existence is never
 * leaked to a non-owner.
 *
 * <p>{@link #createExpense} also publishes {@link ExpenseCreatedEvent} after a successful save,
 * consumed by {@code OverspendAlertListener} (Sprint 5, Batch 3) to check the overspend
 * threshold. Only creation publishes it (MVP scope, see {@code docs/sprints/sprint5.md}) —
 * {@link #updateExpense} does not, even when the amount or date changes.
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            ExpenseMapper expenseMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.expenseMapper = expenseMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpenses(
            Long categoryId,
            LocalDate from,
            LocalDate to,
            PaymentMethodType paymentMethod,
            Pageable pageable
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        Specification<Expense> spec = ExpenseSpecifications.ownedBy(userId)
                .and(ExpenseSpecifications.hasCategory(categoryId))
                .and(ExpenseSpecifications.dateFrom(from))
                .and(ExpenseSpecifications.dateTo(to))
                .and(ExpenseSpecifications.hasPaymentMethod(paymentMethod));

        return expenseRepository.findAll(spec, pageable)
                .map(expenseMapper::toResponse);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Expense expense = expenseMapper.toEntity(request);
        expense.setUser(userRepository.getReferenceById(userId));
        expense.setCategory(resolveOwnedCategory(request.categoryId(), userId));

        Expense savedExpense = expenseRepository.save(expense);
        eventPublisher.publishEvent(new ExpenseCreatedEvent(userId, savedExpense.getId()));
        return expenseMapper.toResponse(savedExpense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, ExpenseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Expense expense = findOwnedExpense(expenseId, userId);

        expenseMapper.updateEntityFromRequest(request, expense);
        expense.setCategory(resolveOwnedCategory(request.categoryId(), userId));

        Expense updatedExpense = expenseRepository.save(expense);
        return expenseMapper.toResponse(updatedExpense);
    }

    @Transactional
    public void deleteExpense(Long expenseId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Expense expense = findOwnedExpense(expenseId, userId);
        expenseRepository.delete(expense);
    }

    private Expense findOwnedExpense(Long expenseId, Long userId) {
        return expenseRepository.findByIdAndUser_Id(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado"));
    }

    private Category resolveOwnedCategory(Long categoryId, Long userId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }
}
