package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.income.IncomeRequest;
import com.smartfinance.backend.dto.income.IncomeResponse;
import com.smartfinance.backend.mapper.IncomeMapper;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.model.Income;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.repository.IncomeRepository;
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
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private IncomeMapper incomeMapper;

    @InjectMocks
    private IncomeService incomeService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createIncomeShouldSaveWithCurrentUserAndCategory() {
        setAuthenticatedUser(1L);
        IncomeRequest request = new IncomeRequest(
                8L,
                new BigDecimal("2500.00"),
                "Salario mensual",
                LocalDate.now(),
                true,
                "Salario"
        );
        Income mappedIncome = new Income();
        Category category = buildCategory(8L, CategoryType.INCOME, buildUser(1L), false);
        Income savedIncome = new Income(
                33L,
                buildUser(1L),
                category,
                new BigDecimal("2500.00"),
                "Salario mensual",
                request.date(),
                true,
                "Salario",
                Instant.now(),
                Instant.now()
        );
        IncomeResponse response = new IncomeResponse(
                33L,
                8L,
                "Salario Cat",
                new BigDecimal("2500.00"),
                "Salario mensual",
                request.date(),
                true,
                "Salario",
                savedIncome.getCreatedAt(),
                savedIncome.getUpdatedAt()
        );

        when(incomeMapper.toEntity(request)).thenReturn(mappedIncome);
        when(categoryRepository.findAccessibleByIdAndUserId(8L, 1L)).thenReturn(Optional.of(category));
        when(incomeRepository.save(mappedIncome)).thenReturn(savedIncome);
        when(incomeMapper.toResponse(savedIncome)).thenReturn(response);

        IncomeResponse createdIncome = incomeService.createIncome(request);

        Assertions.assertEquals(33L, createdIncome.id());
        Assertions.assertEquals(1L, mappedIncome.getUser().getId());
        Assertions.assertEquals(8L, mappedIncome.getCategory().getId());
        Assertions.assertTrue(mappedIncome.isRecurring());
    }

    @Test
    void createIncomeShouldThrowWhenCategoryBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        IncomeRequest request = new IncomeRequest(
                9L,
                new BigDecimal("450.00"),
                "Bono",
                LocalDate.now(),
                false,
                "Bonos"
        );
        when(incomeMapper.toEntity(request)).thenReturn(new Income());
        when(categoryRepository.findAccessibleByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());
        when(categoryRepository.existsById(9L)).thenReturn(true);

        Assertions.assertThrows(AccessDeniedException.class, () -> incomeService.createIncome(request));
    }

    @Test
    void updateIncomeShouldThrowWhenUserIsNotOwner() {
        setAuthenticatedUser(1L);
        IncomeRequest request = new IncomeRequest(
                null,
                new BigDecimal("300.00"),
                "Intereses",
                LocalDate.now(),
                false,
                "Inversiones"
        );
        Income income = new Income();
        income.setId(90L);
        income.setUser(buildUser(2L));
        when(incomeRepository.findById(90L)).thenReturn(Optional.of(income));

        Assertions.assertThrows(AccessDeniedException.class, () -> incomeService.updateIncome(90L, request));
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
