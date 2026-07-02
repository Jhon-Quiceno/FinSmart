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
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.repository.RecurringPaymentRepository;
import com.smartfinance.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringPaymentServiceTest {

    @Mock
    private RecurringPaymentRepository recurringPaymentRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecurringPaymentMapper recurringPaymentMapper;

    @InjectMocks
    private RecurringPaymentService recurringPaymentService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRecurringPaymentShouldSeedNextPaymentDateFromFirstPaymentDateAndActivate() {
        setAuthenticatedUser(1L);
        RecurringPaymentRequest request = new RecurringPaymentRequest(
                "Netflix", BigDecimal.valueOf(15), RecurringFrequency.MONTHLY, LocalDate.of(2026, 7, 10)
        );
        RecurringPayment mappedRecurringPayment = new RecurringPayment();
        RecurringPayment savedRecurringPayment = new RecurringPayment();
        savedRecurringPayment.setId(10L);
        RecurringPaymentResponse response = new RecurringPaymentResponse(
                10L, "Netflix", BigDecimal.valueOf(15), RecurringFrequency.MONTHLY,
                LocalDate.of(2026, 7, 10), true, null, null
        );

        when(recurringPaymentMapper.toEntity(request)).thenReturn(mappedRecurringPayment);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(recurringPaymentRepository.save(mappedRecurringPayment)).thenReturn(savedRecurringPayment);
        when(recurringPaymentMapper.toResponse(savedRecurringPayment)).thenReturn(response);

        RecurringPaymentResponse createdRecurringPayment = recurringPaymentService.createRecurringPayment(request);

        Assertions.assertEquals(10L, createdRecurringPayment.id());
        Assertions.assertEquals(LocalDate.of(2026, 7, 10), mappedRecurringPayment.getNextPaymentDate());
        Assertions.assertTrue(mappedRecurringPayment.isActive());
        Assertions.assertEquals(1L, mappedRecurringPayment.getUser().getId());
    }

    @Test
    void updateRecurringPaymentShouldNotTouchNextPaymentDateOrActiveFlag() {
        setAuthenticatedUser(1L);
        RecurringPayment recurringPayment = buildRecurringPayment(20L, 1L, RecurringFrequency.MONTHLY, LocalDate.of(2026, 8, 1), true);
        RecurringPaymentUpdateRequest request = new RecurringPaymentUpdateRequest("Netflix Premium", BigDecimal.valueOf(20), RecurringFrequency.MONTHLY);

        when(recurringPaymentRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(recurringPayment));
        when(recurringPaymentRepository.save(recurringPayment)).thenReturn(recurringPayment);
        when(recurringPaymentMapper.toResponse(recurringPayment)).thenReturn(
                new RecurringPaymentResponse(20L, "Netflix Premium", BigDecimal.valueOf(20), RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 8, 1), true, null, null)
        );

        RecurringPaymentResponse updatedRecurringPayment = recurringPaymentService.updateRecurringPayment(20L, request);

        Assertions.assertEquals(LocalDate.of(2026, 8, 1), updatedRecurringPayment.nextPaymentDate());
        Assertions.assertTrue(updatedRecurringPayment.isActive());
        Assertions.assertEquals(LocalDate.of(2026, 8, 1), recurringPayment.getNextPaymentDate());
        Assertions.assertTrue(recurringPayment.isActive());
    }

    @Test
    void updateRecurringPaymentShouldThrowNotFoundWhenBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        RecurringPaymentUpdateRequest request = new RecurringPaymentUpdateRequest("Otro nombre", BigDecimal.TEN, RecurringFrequency.WEEKLY);
        when(recurringPaymentRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> recurringPaymentService.updateRecurringPayment(99L, request));
    }

    @Test
    void deleteRecurringPaymentShouldDeleteWhenOwnedByCurrentUser() {
        setAuthenticatedUser(3L);
        RecurringPayment recurringPayment = buildRecurringPayment(55L, 3L, RecurringFrequency.WEEKLY, LocalDate.now(), true);
        when(recurringPaymentRepository.findByIdAndUser_Id(55L, 3L)).thenReturn(Optional.of(recurringPayment));

        recurringPaymentService.deleteRecurringPayment(55L);

        verify(recurringPaymentRepository).delete(recurringPayment);
    }

    @Test
    void deleteRecurringPaymentShouldThrowNotFoundWhenOwnedByAnotherUser() {
        setAuthenticatedUser(3L);
        when(recurringPaymentRepository.findByIdAndUser_Id(55L, 3L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> recurringPaymentService.deleteRecurringPayment(55L));
    }

    @Test
    void toggleRecurringPaymentShouldFlipActiveFlagFromTrueToFalse() {
        setAuthenticatedUser(1L);
        RecurringPayment recurringPayment = buildRecurringPayment(20L, 1L, RecurringFrequency.MONTHLY, LocalDate.of(2026, 8, 1), true);

        when(recurringPaymentRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(recurringPayment));
        when(recurringPaymentRepository.save(recurringPayment)).thenReturn(recurringPayment);
        when(recurringPaymentMapper.toResponse(recurringPayment)).thenReturn(
                new RecurringPaymentResponse(20L, "Netflix", BigDecimal.TEN, RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 8, 1), false, null, null)
        );

        RecurringPaymentResponse result = recurringPaymentService.toggleRecurringPayment(20L);

        Assertions.assertFalse(recurringPayment.isActive());
        Assertions.assertFalse(result.isActive());
    }

    @Test
    void toggleRecurringPaymentShouldFlipActiveFlagFromFalseToTrue() {
        setAuthenticatedUser(1L);
        RecurringPayment recurringPayment = buildRecurringPayment(20L, 1L, RecurringFrequency.MONTHLY, LocalDate.of(2026, 8, 1), false);

        when(recurringPaymentRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(recurringPayment));
        when(recurringPaymentRepository.save(recurringPayment)).thenReturn(recurringPayment);
        when(recurringPaymentMapper.toResponse(recurringPayment)).thenReturn(
                new RecurringPaymentResponse(20L, "Netflix", BigDecimal.TEN, RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 8, 1), true, null, null)
        );

        recurringPaymentService.toggleRecurringPayment(20L);

        Assertions.assertTrue(recurringPayment.isActive());
    }

    @Test
    void payRecurringPaymentShouldCreateLinkedExpenseAndAdvanceMonthlyFromCurrentNextPaymentDate() {
        setAuthenticatedUser(1L);
        RecurringPayment recurringPayment = buildRecurringPayment(20L, 1L, RecurringFrequency.MONTHLY, LocalDate.of(2026, 6, 15), true);
        recurringPayment.setName("Netflix");
        recurringPayment.setAmount(BigDecimal.valueOf(15));

        Expense savedExpense = new Expense();
        savedExpense.setId(77L);

        when(recurringPaymentRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(recurringPayment));
        when(recurringPaymentRepository.advanceNextPaymentDate(20L, LocalDate.of(2026, 6, 15), LocalDate.of(2026, 7, 15)))
                .thenReturn(1);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(org.mockito.ArgumentMatchers.any(Expense.class))).thenReturn(savedExpense);
        when(recurringPaymentRepository.save(recurringPayment)).thenReturn(recurringPayment);
        when(recurringPaymentMapper.toResponse(recurringPayment)).thenReturn(
                new RecurringPaymentResponse(20L, "Netflix", BigDecimal.valueOf(15), RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 7, 15), true, null, null)
        );

        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);

        RecurringPaymentPayResponse result = recurringPaymentService.payRecurringPayment(20L);

        verify(recurringPaymentRepository).advanceNextPaymentDate(20L, LocalDate.of(2026, 6, 15), LocalDate.of(2026, 7, 15));
        verify(expenseRepository).save(expenseCaptor.capture());
        Expense capturedExpense = expenseCaptor.getValue();
        Assertions.assertEquals("Netflix", capturedExpense.getDescription());
        Assertions.assertEquals(BigDecimal.valueOf(15), capturedExpense.getAmount());
        Assertions.assertEquals(LocalDate.now(), capturedExpense.getDate());
        Assertions.assertEquals(PaymentMethodType.OTHER, capturedExpense.getPaymentMethod());
        Assertions.assertNull(capturedExpense.getCategory());
        Assertions.assertEquals(recurringPayment, capturedExpense.getRecurringPayment());

        Assertions.assertEquals(LocalDate.of(2026, 7, 15), recurringPayment.getNextPaymentDate());
        Assertions.assertEquals(77L, result.expenseId());
        Assertions.assertEquals(LocalDate.of(2026, 7, 15), result.recurringPayment().nextPaymentDate());
    }

    @Test
    void payRecurringPaymentShouldAdvanceWeeklyFromCurrentNextPaymentDate() {
        setAuthenticatedUser(1L);
        RecurringPayment recurringPayment = buildRecurringPayment(21L, 1L, RecurringFrequency.WEEKLY, LocalDate.of(2026, 6, 15), true);
        recurringPayment.setName("Gimnasio");
        recurringPayment.setAmount(BigDecimal.valueOf(30));

        Expense savedExpense = new Expense();
        savedExpense.setId(78L);

        when(recurringPaymentRepository.findByIdAndUser_Id(21L, 1L)).thenReturn(Optional.of(recurringPayment));
        when(recurringPaymentRepository.advanceNextPaymentDate(21L, LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 22)))
                .thenReturn(1);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(org.mockito.ArgumentMatchers.any(Expense.class))).thenReturn(savedExpense);
        when(recurringPaymentRepository.save(recurringPayment)).thenReturn(recurringPayment);
        when(recurringPaymentMapper.toResponse(recurringPayment)).thenReturn(
                new RecurringPaymentResponse(21L, "Gimnasio", BigDecimal.valueOf(30), RecurringFrequency.WEEKLY,
                        LocalDate.of(2026, 6, 22), true, null, null)
        );

        recurringPaymentService.payRecurringPayment(21L);

        Assertions.assertEquals(LocalDate.of(2026, 6, 22), recurringPayment.getNextPaymentDate());
    }

    @Test
    void payRecurringPaymentShouldNotCompressScheduleWhenExecutedLate() {
        // A late execution must recalculate from the CURRENT nextPaymentDate, not from today,
        // so it must NOT equal LocalDate.now().plusMonths(1) unless that happens to coincide.
        setAuthenticatedUser(1L);
        LocalDate longOverdueNextPaymentDate = LocalDate.of(2020, 1, 15);
        LocalDate expectedNewNextPaymentDate = LocalDate.of(2020, 2, 15);
        RecurringPayment recurringPayment = buildRecurringPayment(22L, 1L, RecurringFrequency.MONTHLY, longOverdueNextPaymentDate, true);
        recurringPayment.setName("Seguro");
        recurringPayment.setAmount(BigDecimal.valueOf(50));

        Expense savedExpense = new Expense();
        savedExpense.setId(79L);

        when(recurringPaymentRepository.findByIdAndUser_Id(22L, 1L)).thenReturn(Optional.of(recurringPayment));
        when(recurringPaymentRepository.advanceNextPaymentDate(22L, longOverdueNextPaymentDate, expectedNewNextPaymentDate))
                .thenReturn(1);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(org.mockito.ArgumentMatchers.any(Expense.class))).thenReturn(savedExpense);
        when(recurringPaymentRepository.save(recurringPayment)).thenReturn(recurringPayment);
        when(recurringPaymentMapper.toResponse(recurringPayment)).thenReturn(
                new RecurringPaymentResponse(22L, "Seguro", BigDecimal.valueOf(50), RecurringFrequency.MONTHLY,
                        expectedNewNextPaymentDate, true, null, null)
        );

        recurringPaymentService.payRecurringPayment(22L);

        Assertions.assertEquals(expectedNewNextPaymentDate, recurringPayment.getNextPaymentDate());
    }

    @Test
    void payRecurringPaymentShouldThrowConflictAndNotCreateExpenseWhenAlreadyPaidConcurrently() {
        // Regression test for the duplicate-/pay race: another concurrent execution already
        // advanced nextPaymentDate in the database (advanceNextPaymentDate affects zero rows),
        // so this call must reject with a 409-mapped exception and must NOT create an Expense.
        setAuthenticatedUser(1L);
        RecurringPayment recurringPayment = buildRecurringPayment(20L, 1L, RecurringFrequency.MONTHLY, LocalDate.of(2026, 6, 15), true);
        recurringPayment.setName("Netflix");
        recurringPayment.setAmount(BigDecimal.valueOf(15));

        when(recurringPaymentRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(recurringPayment));
        when(recurringPaymentRepository.advanceNextPaymentDate(20L, LocalDate.of(2026, 6, 15), LocalDate.of(2026, 7, 15)))
                .thenReturn(0);

        Assertions.assertThrows(
                RecurringPaymentAlreadyPaidException.class,
                () -> recurringPaymentService.payRecurringPayment(20L)
        );
        verifyNoInteractions(expenseRepository);
        // Only the failed atomic advance ran; no subsequent .save() persisting a "new" state.
        verify(recurringPaymentRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void payRecurringPaymentShouldThrowNotFoundWhenBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        when(recurringPaymentRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> recurringPaymentService.payRecurringPayment(99L));
    }

    @Test
    void getRecurringPaymentsShouldReturnPagedResultsForCurrentUser() {
        setAuthenticatedUser(1L);
        Pageable pageable = PageRequest.of(0, 20);
        RecurringPayment recurringPayment = new RecurringPayment();
        Page<RecurringPayment> page = new PageImpl<>(List.of(recurringPayment), pageable, 1);
        RecurringPaymentResponse response = new RecurringPaymentResponse(
                1L, "Netflix", BigDecimal.TEN, RecurringFrequency.MONTHLY, LocalDate.now(), true, null, null
        );

        when(recurringPaymentRepository.findAllByUser_Id(1L, pageable)).thenReturn(page);
        when(recurringPaymentMapper.toResponse(recurringPayment)).thenReturn(response);

        Page<RecurringPaymentResponse> result = recurringPaymentService.getRecurringPayments(pageable);

        Assertions.assertEquals(1, result.getTotalElements());
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

    private RecurringPayment buildRecurringPayment(Long id, Long userId, RecurringFrequency frequency, LocalDate nextPaymentDate, boolean active) {
        RecurringPayment recurringPayment = new RecurringPayment();
        recurringPayment.setId(id);
        recurringPayment.setUser(buildUser(userId));
        recurringPayment.setFrequency(frequency);
        recurringPayment.setNextPaymentDate(nextPaymentDate);
        recurringPayment.setActive(active);
        return recurringPayment;
    }
}
