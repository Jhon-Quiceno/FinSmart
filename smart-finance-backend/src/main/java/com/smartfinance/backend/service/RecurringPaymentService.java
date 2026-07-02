package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.recurring.RecurringPaymentPayResponse;
import com.smartfinance.backend.dto.recurring.RecurringPaymentRequest;
import com.smartfinance.backend.dto.recurring.RecurringPaymentResponse;
import com.smartfinance.backend.dto.recurring.RecurringPaymentUpdateRequest;
import com.smartfinance.backend.exception.RecurringPaymentAlreadyPaidException;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.RecurringPaymentMapper;
import com.smartfinance.backend.model.Expense;
import com.smartfinance.backend.model.PaymentMethodType;
import com.smartfinance.backend.model.RecurringFrequency;
import com.smartfinance.backend.model.RecurringPayment;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.repository.RecurringPaymentRepository;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Business logic for managing the current user's {@link RecurringPayment} records.
 *
 * <p>Every operation resolves the caller via {@link SecurityUtils#getCurrentUserId()} and
 * scopes reads/writes strictly to that user. Mutations on a recurring payment owned by
 * another user raise {@link ResourceNotFoundException} (HTTP 404) rather than a 403.
 *
 * <p>{@link #payRecurringPayment} creates an {@link Expense} linked back to the recurring
 * payment (see {@link Expense#getRecurringPayment()}) and recalculates
 * {@code nextPaymentDate} from the <em>current</em> {@code nextPaymentDate} — never from
 * {@link LocalDate#now()} — so a late execution does not compress the following cycle. The date
 * advance is applied first via {@link RecurringPaymentRepository#advanceNextPaymentDate} (an
 * atomic conditional {@code UPDATE}, not a read-then-write), so a duplicate {@code /pay} call
 * (client retry, double-click, two tabs) cannot race past this guard and create a second
 * {@link Expense} for the same logical payment — a losing call gets
 * {@link RecurringPaymentAlreadyPaidException} (HTTP 409) instead.
 */
@Service
public class RecurringPaymentService {

    private final RecurringPaymentRepository recurringPaymentRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final RecurringPaymentMapper recurringPaymentMapper;

    public RecurringPaymentService(
            RecurringPaymentRepository recurringPaymentRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            RecurringPaymentMapper recurringPaymentMapper
    ) {
        this.recurringPaymentRepository = recurringPaymentRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.recurringPaymentMapper = recurringPaymentMapper;
    }

    @Transactional(readOnly = true)
    public Page<RecurringPaymentResponse> getRecurringPayments(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return recurringPaymentRepository.findAllByUser_Id(userId, pageable)
                .map(recurringPaymentMapper::toResponse);
    }

    @Transactional
    public RecurringPaymentResponse createRecurringPayment(RecurringPaymentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        RecurringPayment recurringPayment = recurringPaymentMapper.toEntity(request);
        recurringPayment.setUser(userRepository.getReferenceById(userId));
        recurringPayment.setNextPaymentDate(request.firstPaymentDate());
        recurringPayment.setActive(true);

        RecurringPayment savedRecurringPayment = recurringPaymentRepository.save(recurringPayment);
        return recurringPaymentMapper.toResponse(savedRecurringPayment);
    }

    @Transactional
    public RecurringPaymentResponse updateRecurringPayment(Long recurringPaymentId, RecurringPaymentUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        RecurringPayment recurringPayment = findOwnedRecurringPayment(recurringPaymentId, userId);

        recurringPaymentMapper.updateEntityFromRequest(request, recurringPayment);

        RecurringPayment updatedRecurringPayment = recurringPaymentRepository.save(recurringPayment);
        return recurringPaymentMapper.toResponse(updatedRecurringPayment);
    }

    @Transactional
    public void deleteRecurringPayment(Long recurringPaymentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        RecurringPayment recurringPayment = findOwnedRecurringPayment(recurringPaymentId, userId);
        recurringPaymentRepository.delete(recurringPayment);
    }

    @Transactional
    public RecurringPaymentResponse toggleRecurringPayment(Long recurringPaymentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        RecurringPayment recurringPayment = findOwnedRecurringPayment(recurringPaymentId, userId);

        recurringPayment.setActive(!recurringPayment.isActive());

        RecurringPayment updatedRecurringPayment = recurringPaymentRepository.save(recurringPayment);
        return recurringPaymentMapper.toResponse(updatedRecurringPayment);
    }

    @Transactional
    public RecurringPaymentPayResponse payRecurringPayment(Long recurringPaymentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        RecurringPayment recurringPayment = findOwnedRecurringPayment(recurringPaymentId, userId);

        LocalDate currentNextPaymentDate = recurringPayment.getNextPaymentDate();
        LocalDate newNextPaymentDate = computeNextPaymentDate(recurringPayment);

        int updatedRows = recurringPaymentRepository.advanceNextPaymentDate(
                recurringPaymentId, currentNextPaymentDate, newNextPaymentDate
        );
        if (updatedRows == 0) {
            // Another concurrent /pay call already advanced nextPaymentDate first; do not
            // create a duplicate Expense for the same logical payment.
            throw new RecurringPaymentAlreadyPaidException("Este servicio ya fue marcado como pagado");
        }

        Expense expense = new Expense();
        expense.setUser(userRepository.getReferenceById(userId));
        expense.setDescription(recurringPayment.getName());
        expense.setAmount(recurringPayment.getAmount());
        expense.setDate(LocalDate.now());
        expense.setPaymentMethod(PaymentMethodType.OTHER);
        expense.setCategory(null);
        expense.setRecurringPayment(recurringPayment);
        Expense savedExpense = expenseRepository.save(expense);

        // The atomic UPDATE above already persisted the new date; keep the in-memory entity in
        // sync (rather than relying on a stale nextPaymentDate) so the response DTO reflects
        // the real post-advance value.
        recurringPayment.setNextPaymentDate(newNextPaymentDate);
        RecurringPayment updatedRecurringPayment = recurringPaymentRepository.save(recurringPayment);

        return new RecurringPaymentPayResponse(
                recurringPaymentMapper.toResponse(updatedRecurringPayment),
                savedExpense.getId()
        );
    }

    private LocalDate computeNextPaymentDate(RecurringPayment recurringPayment) {
        LocalDate currentNextPaymentDate = recurringPayment.getNextPaymentDate();
        return recurringPayment.getFrequency() == RecurringFrequency.MONTHLY
                ? currentNextPaymentDate.plusMonths(1)
                : currentNextPaymentDate.plusWeeks(1);
    }

    private RecurringPayment findOwnedRecurringPayment(Long recurringPaymentId, Long userId) {
        return recurringPaymentRepository.findByIdAndUser_Id(recurringPaymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago recurrente no encontrado"));
    }
}
