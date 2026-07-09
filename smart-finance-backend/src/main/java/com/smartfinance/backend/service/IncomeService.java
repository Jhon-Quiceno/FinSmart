package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.income.IncomeRequest;
import com.smartfinance.backend.dto.income.IncomeResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.IncomeMapper;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.Income;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.repository.IncomeRepository;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.repository.specification.IncomeSpecifications;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for managing the current user's {@link Income} records.
 *
 * <p>Every operation resolves the caller via {@link SecurityUtils#getCurrentUserId()} and
 * scopes reads/writes strictly to that user. Mutations on an income owned by another user,
 * or a {@code categoryId} referencing a category owned by another user, raise
 * {@link ResourceNotFoundException} (HTTP 404) rather than a 403, so existence is never
 * leaked to a non-owner.
 */
@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final IncomeMapper incomeMapper;

    public IncomeService(
            IncomeRepository incomeRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            IncomeMapper incomeMapper
    ) {
        this.incomeRepository = incomeRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.incomeMapper = incomeMapper;
    }

    @Transactional(readOnly = true)
    public Page<IncomeResponse> getIncomes(Integer month, Integer year, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Specification<Income> spec = IncomeSpecifications.ownedBy(userId)
                .and(IncomeSpecifications.inPeriod(month, year));

        return incomeRepository.findAll(spec, pageable)
                .map(incomeMapper::toResponse);
    }

    @Transactional
    public IncomeResponse createIncome(IncomeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Income income = incomeMapper.toEntity(request);
        income.setUser(userRepository.getReferenceById(userId));
        income.setCategory(resolveOwnedCategory(request.categoryId(), userId));

        Income savedIncome = incomeRepository.save(income);
        return incomeMapper.toResponse(savedIncome);
    }

    @Transactional
    public IncomeResponse updateIncome(Long incomeId, IncomeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Income income = findOwnedIncome(incomeId, userId);

        incomeMapper.updateEntityFromRequest(request, income);
        income.setCategory(resolveOwnedCategory(request.categoryId(), userId));

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
        return incomeRepository.findByIdAndUser_Id(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso no encontrado"));
    }

    private Category resolveOwnedCategory(Long categoryId, Long userId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }
}
