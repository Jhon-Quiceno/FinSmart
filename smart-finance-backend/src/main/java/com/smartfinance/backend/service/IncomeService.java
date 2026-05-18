package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.income.IncomeRequest;
import com.smartfinance.backend.dto.income.IncomeResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.IncomeMapper;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.model.Income;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.repository.IncomeRepository;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final IncomeMapper incomeMapper;

    public IncomeService(
            IncomeRepository incomeRepository,
            CategoryRepository categoryRepository,
            IncomeMapper incomeMapper
    ) {
        this.incomeRepository = incomeRepository;
        this.categoryRepository = categoryRepository;
        this.incomeMapper = incomeMapper;
    }

    @Transactional(readOnly = true)
    public Page<IncomeResponse> getIncomes(Integer month, Integer year, Long categoryId, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate fromDate = null;
        LocalDate toDate = null;

        if (month != null || year != null) {
            if (month == null || year == null) {
                throw new IllegalArgumentException("Los filtros month y year deben enviarse juntos");
            }

            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
            }

            YearMonth yearMonth = YearMonth.of(year, month);
            fromDate = yearMonth.atDay(1);
            toDate = yearMonth.atEndOfMonth();
        }

        return incomeRepository.findAllByFilters(userId, categoryId, fromDate, toDate, pageable)
                .map(incomeMapper::toResponse);
    }

    @Transactional
    public IncomeResponse createIncome(IncomeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Income income = incomeMapper.toEntity(request);
        income.setUser(buildUserReference(userId));
        income.setCategory(resolveCategory(request.categoryId(), userId, CategoryType.INCOME));
        income.setRecurring(Boolean.TRUE.equals(request.isRecurring()));

        Income savedIncome = incomeRepository.save(income);
        return incomeMapper.toResponse(savedIncome);
    }

    @Transactional
    public IncomeResponse updateIncome(Long incomeId, IncomeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Income income = findOwnedIncome(incomeId, userId);

        incomeMapper.updateEntityFromRequest(request, income);
        income.setCategory(resolveCategory(request.categoryId(), userId, CategoryType.INCOME));
        income.setRecurring(Boolean.TRUE.equals(request.isRecurring()));

        Income updatedIncome = incomeRepository.save(income);
        return incomeMapper.toResponse(updatedIncome);
    }

    @Transactional
    public void deleteIncome(Long incomeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Income income = findOwnedIncome(incomeId, userId);
        incomeRepository.delete(income);
    }

    private Income findOwnedIncome(Long incomeId, Long userId) {
        Income income = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso no encontrado"));

        if (!userId.equals(income.getUser().getId())) {
            throw new AccessDeniedException("No tienes permisos sobre este ingreso");
        }

        return income;
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
