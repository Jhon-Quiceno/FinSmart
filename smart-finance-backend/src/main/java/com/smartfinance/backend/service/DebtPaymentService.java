package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.debt.DebtPaymentRequest;
import com.smartfinance.backend.dto.debt.DebtPaymentResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.DebtPaymentMapper;
import com.smartfinance.backend.model.Debt;
import com.smartfinance.backend.model.DebtPayment;
import com.smartfinance.backend.repository.DebtPaymentRepository;
import com.smartfinance.backend.repository.DebtRepository;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Business logic for registering and listing {@link DebtPayment} records against a
 * {@link Debt} owned by the current user.
 *
 * <p>{@link #createPayment} is the only place {@link Debt#getRemainingAmount()} is decremented
 * — it validates the debt belongs to the current user, rejects a payment amount greater than
 * the debt's current remaining balance, then saves both the new payment and the updated debt
 * in the same transaction.
 */
@Service
public class DebtPaymentService {

    private final DebtPaymentRepository debtPaymentRepository;
    private final DebtRepository debtRepository;
    private final DebtPaymentMapper debtPaymentMapper;

    public DebtPaymentService(
            DebtPaymentRepository debtPaymentRepository,
            DebtRepository debtRepository,
            DebtPaymentMapper debtPaymentMapper
    ) {
        this.debtPaymentRepository = debtPaymentRepository;
        this.debtRepository = debtRepository;
        this.debtPaymentMapper = debtPaymentMapper;
    }

    @Transactional(readOnly = true)
    public Page<DebtPaymentResponse> getPayments(Long debtId, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        findOwnedDebt(debtId, userId);

        return debtPaymentRepository.findAllByDebt_IdOrderByPaymentDateDescIdDesc(debtId, pageable)
                .map(debtPaymentMapper::toResponse);
    }

    @Transactional
    public DebtPaymentResponse createPayment(Long debtId, DebtPaymentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Debt debt = findOwnedDebt(debtId, userId);

        if (request.amount().compareTo(debt.getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("El abono no puede superar el saldo restante de la deuda");
        }

        DebtPayment payment = debtPaymentMapper.toEntity(request);
        payment.setDebt(debt);
        payment.setPaymentDate(request.paymentDate() != null ? request.paymentDate() : LocalDate.now());

        debt.setRemainingAmount(debt.getRemainingAmount().subtract(request.amount()));
        debtRepository.save(debt);

        DebtPayment savedPayment = debtPaymentRepository.save(payment);
        return debtPaymentMapper.toResponse(savedPayment);
    }

    private Debt findOwnedDebt(Long debtId, Long userId) {
        return debtRepository.findByIdAndUser_Id(debtId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deuda no encontrada"));
    }
}
