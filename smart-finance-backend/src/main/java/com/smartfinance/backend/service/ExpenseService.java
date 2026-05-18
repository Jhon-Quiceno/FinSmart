package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.expense.ExpenseRequest;
import com.smartfinance.backend.dto.expense.ExpenseResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.ExpenseMapper;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.model.Expense;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseMapper expenseMapper;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            CategoryRepository categoryRepository,
            ExpenseMapper expenseMapper
    ) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.expenseMapper = expenseMapper;
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpenses(Long categoryId, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser mayor a la fecha final");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        return expenseRepository.findAllByFilters(userId, categoryId, fromDate, toDate, pageable)
                .map(expenseMapper::toResponse);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Expense expense = expenseMapper.toEntity(request);
        expense.setUser(buildUserReference(userId));
        expense.setCategory(resolveCategory(request.categoryId(), userId, CategoryType.EXPENSE));
        expense.setRecurring(Boolean.TRUE.equals(request.isRecurring()));

        Expense savedExpense = expenseRepository.save(expense);
        return expenseMapper.toResponse(savedExpense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, ExpenseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Expense expense = findOwnedExpense(expenseId, userId);

        expenseMapper.updateEntityFromRequest(request, expense);
        expense.setCategory(resolveCategory(request.categoryId(), userId, CategoryType.EXPENSE));
        expense.setRecurring(Boolean.TRUE.equals(request.isRecurring()));

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
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado"));

        if (!userId.equals(expense.getUser().getId())) {
            throw new AccessDeniedException("No tienes permisos sobre este gasto");
        }

        return expense;
    }

    private Category resolveCategory(Long categoryId, Long userId, CategoryType expectedType) {
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

        if (category.getType() != expectedType) {
            throw new IllegalArgumentException("La categoría seleccionada no corresponde al tipo de movimiento");
        }

        return category;
    }

    private User buildUserReference(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
