package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.expense.ExpenseRequest;
import com.smartfinance.backend.dto.expense.ExpenseResponse;
import com.smartfinance.backend.mapper.ExpenseMapper;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.model.Expense;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.repository.ExpenseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseService expenseService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createExpenseShouldSaveWithCurrentUserAndCategory() {
        setAuthenticatedUser(1L);
        ExpenseRequest request = new ExpenseRequest(
                4L,
                new BigDecimal("180.50"),
                "Supermercado",
                LocalDate.now(),
                false,
                "Tarjeta de Débito"
        );
        Expense mappedExpense = new Expense();
        Category category = buildCategory(4L, CategoryType.EXPENSE, buildUser(1L), false);
        Expense savedExpense = new Expense(
                12L,
                buildUser(1L),
                category,
                new BigDecimal("180.50"),
                "Supermercado",
                request.date(),
                false,
                "Tarjeta de Débito",
                Instant.now(),
                Instant.now()
        );
        ExpenseResponse response = new ExpenseResponse(
                12L,
                4L,
                "Supermercado Cat",
                new BigDecimal("180.50"),
                "Supermercado",
                request.date(),
                false,
                "Tarjeta de Débito",
                savedExpense.getCreatedAt(),
                savedExpense.getUpdatedAt()
        );

        when(expenseMapper.toEntity(request)).thenReturn(mappedExpense);
        when(categoryRepository.findAccessibleByIdAndUserId(4L, 1L)).thenReturn(Optional.of(category));
        when(expenseRepository.save(mappedExpense)).thenReturn(savedExpense);
        when(expenseMapper.toResponse(savedExpense)).thenReturn(response);

        ExpenseResponse createdExpense = expenseService.createExpense(request);

        Assertions.assertEquals(12L, createdExpense.id());
        Assertions.assertEquals(1L, mappedExpense.getUser().getId());
        Assertions.assertEquals(4L, mappedExpense.getCategory().getId());
        Assertions.assertFalse(mappedExpense.isRecurring());
    }

    @Test
    void createExpenseShouldThrowWhenCategoryBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        ExpenseRequest request = new ExpenseRequest(
                7L,
                new BigDecimal("55.00"),
                "Taxi",
                LocalDate.now(),
                false,
                "Efectivo"
        );
        when(expenseMapper.toEntity(request)).thenReturn(new Expense());
        when(categoryRepository.findAccessibleByIdAndUserId(7L, 1L)).thenReturn(Optional.empty());
        when(categoryRepository.existsById(7L)).thenReturn(true);

        Assertions.assertThrows(AccessDeniedException.class, () -> expenseService.createExpense(request));
    }

    @Test
    void updateExpenseShouldThrowWhenUserIsNotOwner() {
        setAuthenticatedUser(1L);
        ExpenseRequest request = new ExpenseRequest(
                null,
                new BigDecimal("20.00"),
                "Pasaje",
                LocalDate.now(),
                false,
                "Efectivo"
        );
        Expense expense = new Expense();
        expense.setId(88L);
        expense.setUser(buildUser(2L));
        when(expenseRepository.findById(88L)).thenReturn(Optional.of(expense));

        Assertions.assertThrows(AccessDeniedException.class, () -> expenseService.updateExpense(88L, request));
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }

    private User buildUser(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private Category buildCategory(Long categoryId, CategoryType type, User user, boolean isSystem) {
        Category category = new Category();
        category.setId(categoryId);
        category.setType(type);
        category.setUser(user);
        category.setSystem(isSystem);
        return category;
    }
}
