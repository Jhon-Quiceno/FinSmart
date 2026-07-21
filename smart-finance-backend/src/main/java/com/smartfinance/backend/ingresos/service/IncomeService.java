package com.smartfinance.backend.ingresos.service;

import com.smartfinance.backend.ingresos.model.dto.IncomeRequest;
import com.smartfinance.backend.ingresos.model.dto.IncomeResponse;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.ingresos.mapper.IncomeMapper;
import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.ingresos.model.entity.Income;
import com.smartfinance.backend.gastos.repository.CategoryRepository;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.ingresos.repository.specification.IncomeSpecifications;
import com.smartfinance.backend.common.security.SecurityUtils;
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
        return createIncome(SecurityUtils.getCurrentUserId(), request);
    }

    /**
     * Same as {@link #createIncome(IncomeRequest)} but for a caller that already resolved
     * {@code userId} itself (e.g. {@code StatementImportService#confirm}, which creates several
     * incomes for the same user in one request and would otherwise re-read the security context
     * on every row).
     */
    @Transactional
    public IncomeResponse createIncome(Long userId, IncomeRequest request) {
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
