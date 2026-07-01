package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.recurring.RecurringPaymentPayResponse;
import com.smartfinance.backend.dto.recurring.RecurringPaymentRequest;
import com.smartfinance.backend.dto.recurring.RecurringPaymentResponse;
import com.smartfinance.backend.dto.recurring.RecurringPaymentUpdateRequest;
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
 * {@link LocalDate#now()} — so a late execution does not compress the following cycle. Both
 * the new expense and the updated recurring payment are saved in the same transaction.
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

        Expense expense = new Expense();
        expense.setUser(userRepository.getReferenceById(userId));
        expense.setDescription(recurringPayment.getName());
        expense.setAmount(recurringPayment.getAmount());
        expense.setDate(LocalDate.now());
        expense.setPaymentMethod(PaymentMethodType.OTHER);
        expense.setCategory(null);
        expense.setRecurringPayment(recurringPayment);
        Expense savedExpense = expenseRepository.save(expense);

        recurringPayment.setNextPaymentDate(computeNextPaymentDate(recurringPayment));
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
