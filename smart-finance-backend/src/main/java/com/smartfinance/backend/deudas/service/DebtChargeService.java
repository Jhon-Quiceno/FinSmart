package com.smartfinance.backend.deudas.service;

import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.deudas.mapper.DebtChargeMapper;
import com.smartfinance.backend.deudas.mapper.DebtMapper;
import com.smartfinance.backend.deudas.model.dto.DebtChargeRequest;
import com.smartfinance.backend.deudas.model.dto.DebtChargeResponse;
import com.smartfinance.backend.deudas.model.dto.DebtResponse;
import com.smartfinance.backend.deudas.model.entity.Debt;
import com.smartfinance.backend.deudas.model.entity.DebtCharge;
import com.smartfinance.backend.deudas.repository.DebtChargeRepository;
import com.smartfinance.backend.deudas.repository.DebtRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Business logic for registering and listing {@link DebtCharge} records against a
 * {@link Debt} owned by the current user.
 *
 * <p>{@link #createCharge} is the mirror of {@code DebtPaymentService#createPayment}: instead of
 * decrementing {@link Debt#getRemainingAmount()}, it increments it via
 * {@link DebtRepository#incrementRemainingAmount} (an atomic {@code UPDATE}, not a
 * read-then-write) before persisting the {@link DebtCharge}.
 *
 * <p>Unlike {@code DebtPaymentService#createPayment}, this does NOT create a linked
 * {@link com.smartfinance.backend.gastos.model.entity.Expense} — a charge increasing a debt's
 * balance is not a cash expense, and Fase A of the debt redesign keeps this deliberately simple
 * (see {@code docs/rediseno-deudas-tarjetas.md}).
 */
@Service
public class DebtChargeService {

    private final DebtChargeRepository debtChargeRepository;
    private final DebtRepository debtRepository;
    private final DebtChargeMapper debtChargeMapper;
    private final DebtMapper debtMapper;

    public DebtChargeService(
            DebtChargeRepository debtChargeRepository,
            DebtRepository debtRepository,
            DebtChargeMapper debtChargeMapper,
            DebtMapper debtMapper
    ) {
        this.debtChargeRepository = debtChargeRepository;
        this.debtRepository = debtRepository;
        this.debtChargeMapper = debtChargeMapper;
        this.debtMapper = debtMapper;
    }

    @Transactional(readOnly = true)
    public Page<DebtChargeResponse> getCharges(Long debtId, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        findOwnedDebt(debtId, userId);

        return debtChargeRepository.findAllByDebt_IdOrderByChargeDateDescIdDesc(debtId, pageable)
                .map(debtChargeMapper::toResponse);
    }

    @Transactional
    public DebtResponse createCharge(Long debtId, DebtChargeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        findOwnedDebt(debtId, userId);

        int updatedRows = debtRepository.incrementRemainingAmount(debtId, request.amount());
        if (updatedRows == 0) {
            // The debt was deleted concurrently between the ownership check above and this
            // update; reject without persisting the DebtCharge.
            throw new ResourceNotFoundException("Deuda no encontrada");
        }

        // Re-read after the bulk UPDATE (which cleared the persistence context) so the debt
        // reflects the new remainingAmount instead of the stale value loaded above.
        Debt updatedDebt = debtRepository.findById(debtId)
                .orElseThrow(() -> new ResourceNotFoundException("Deuda no encontrada"));

        DebtCharge charge = debtChargeMapper.toEntity(request);
        charge.setDebt(updatedDebt);
        LocalDate chargeDate = request.chargeDate() != null ? request.chargeDate() : LocalDate.now();
        charge.setChargeDate(chargeDate);
        debtChargeRepository.save(charge);

        return debtMapper.toResponse(updatedDebt);
    }

    private Debt findOwnedDebt(Long debtId, Long userId) {
        return debtRepository.findByIdAndUser_Id(debtId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deuda no encontrada"));
    }
}
