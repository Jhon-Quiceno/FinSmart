package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.debt.DebtRequest;
import com.smartfinance.backend.dto.debt.DebtResponse;
import com.smartfinance.backend.dto.debt.DebtUpdateRequest;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.DebtMapper;
import com.smartfinance.backend.model.Debt;
import com.smartfinance.backend.repository.DebtRepository;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for managing the current user's {@link Debt} records.
 *
 * <p>Every operation resolves the caller via {@link SecurityUtils#getCurrentUserId()} and
 * scopes reads/writes strictly to that user. Mutations on a debt owned by another user raise
 * {@link ResourceNotFoundException} (HTTP 404) rather than a 403, so debt existence is never
 * leaked to a non-owner.
 *
 * <p>{@link #updateDebt} deliberately cannot change {@code totalAmount} or
 * {@code remainingAmount} — see {@link DebtUpdateRequest}'s Javadoc. The only place
 * {@code remainingAmount} changes after creation is {@code DebtPaymentService#createPayment}.
 */
@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final UserRepository userRepository;
    private final DebtMapper debtMapper;

    public DebtService(DebtRepository debtRepository, UserRepository userRepository, DebtMapper debtMapper) {
        this.debtRepository = debtRepository;
        this.userRepository = userRepository;
        this.debtMapper = debtMapper;
    }

    @Transactional(readOnly = true)
    public Page<DebtResponse> getDebts(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return debtRepository.findAllByUser_Id(userId, pageable)
                .map(debtMapper::toResponse);
    }

    @Transactional
    public DebtResponse createDebt(DebtRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Debt debt = debtMapper.toEntity(request);
        debt.setUser(userRepository.getReferenceById(userId));
        debt.setRemainingAmount(request.totalAmount());

        Debt savedDebt = debtRepository.save(debt);
        return debtMapper.toResponse(savedDebt);
    }

    @Transactional
    public DebtResponse updateDebt(Long debtId, DebtUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Debt debt = findOwnedDebt(debtId, userId);

        debtMapper.updateEntityFromRequest(request, debt);

        Debt updatedDebt = debtRepository.save(debt);
        return debtMapper.toResponse(updatedDebt);
    }

    @Transactional
    public void deleteDebt(Long debtId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Debt debt = findOwnedDebt(debtId, userId);
        debtRepository.delete(debt);
    }

    private Debt findOwnedDebt(Long debtId, Long userId) {
        return debtRepository.findByIdAndUser_Id(debtId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deuda no encontrada"));
    }
}
