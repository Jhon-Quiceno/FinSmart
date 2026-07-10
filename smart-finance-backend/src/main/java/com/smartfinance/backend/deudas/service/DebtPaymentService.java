package com.smartfinance.backend.deudas.service;

import com.smartfinance.backend.deudas.model.dto.DebtPaymentRequest;
import com.smartfinance.backend.deudas.model.dto.DebtPaymentResponse;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.deudas.mapper.DebtPaymentMapper;
import com.smartfinance.backend.deudas.model.entity.Debt;
import com.smartfinance.backend.deudas.model.entity.DebtPayment;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.deudas.repository.DebtPaymentRepository;
import com.smartfinance.backend.deudas.repository.DebtRepository;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.common.security.SecurityUtils;
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
 * — it validates the debt belongs to the current user, then decrements the remaining balance
 * via {@link DebtRepository#decrementRemainingAmount} (an atomic conditional {@code UPDATE},
 * not a read-then-write) before persisting the {@link DebtPayment}, so two concurrent payments
 * against the same debt cannot both pass validation against the same stale balance and cause a
 * lost update.
 *
 * <p>{@link #createPayment} also creates an {@link Expense} linked back to the debt payment
 * (see {@link Expense#getDebtPayment()}), mirroring how
 * {@code RecurringPaymentService#payRecurringPayment} generates an {@link Expense} for each
 * recurring payment execution.
 */
@Service
public class DebtPaymentService {

    private final DebtPaymentRepository debtPaymentRepository;
    private final DebtRepository debtRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final DebtPaymentMapper debtPaymentMapper;

    public DebtPaymentService(
            DebtPaymentRepository debtPaymentRepository,
            DebtRepository debtRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            DebtPaymentMapper debtPaymentMapper
    ) {
        this.debtPaymentRepository = debtPaymentRepository;
        this.debtRepository = debtRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
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

        // Cheap fast-path check for a clearer error on the common case. This is NOT the real
        // enforcement — debt.getRemainingAmount() here can be stale under concurrent payments,
        // which is exactly the race the atomic UPDATE below closes.
        if (request.amount().compareTo(debt.getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("El abono no puede superar el saldo restante de la deuda");
        }

        int updatedRows = debtRepository.decrementRemainingAmount(debtId, request.amount());
        if (updatedRows == 0) {
            // Another concurrent payment consumed the remaining balance between our read above
            // and this atomic update; reject without persisting the DebtPayment.
            throw new IllegalArgumentException("El abono no puede superar el saldo restante de la deuda");
        }

        DebtPayment payment = debtPaymentMapper.toEntity(request);
        payment.setDebt(debt);
        LocalDate paymentDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();
        payment.setPaymentDate(paymentDate);

        DebtPayment savedPayment = debtPaymentRepository.save(payment);

        Expense expense = new Expense();
        expense.setUser(userRepository.getReferenceById(userId));
        expense.setDescription("Abono a deuda: " + debt.getName());
        expense.setAmount(request.amount());
        expense.setDate(paymentDate);
        expense.setPaymentMethod(PaymentMethodType.OTHER);
        expense.setCategory(null);
        expense.setDebtPayment(savedPayment);
        Expense savedExpense = expenseRepository.save(expense);

        DebtPaymentResponse mappedResponse = debtPaymentMapper.toResponse(savedPayment);
        return new DebtPaymentResponse(
                mappedResponse.id(),
                mappedResponse.debtId(),
                mappedResponse.amount(),
                mappedResponse.paymentDate(),
                mappedResponse.note(),
                mappedResponse.createdAt(),
                savedExpense.getId()
        );
    }

    private Debt findOwnedDebt(Long debtId, Long userId) {
        return debtRepository.findByIdAndUser_Id(debtId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deuda no encontrada"));
    }
}
