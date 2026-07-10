package com.smartfinance.backend.deudas.service;

import com.smartfinance.backend.deudas.model.dto.DebtPaymentRequest;
import com.smartfinance.backend.deudas.model.dto.DebtPaymentResponse;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.deudas.mapper.DebtPaymentMapper;
import com.smartfinance.backend.deudas.model.entity.Debt;
import com.smartfinance.backend.deudas.model.entity.DebtPayment;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.usuario.model.entity.User;
import com.smartfinance.backend.deudas.repository.DebtPaymentRepository;
import com.smartfinance.backend.deudas.repository.DebtRepository;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtPaymentServiceTest {

    @Mock
    private DebtPaymentRepository debtPaymentRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DebtPaymentMapper debtPaymentMapper;

    @InjectMocks
    private DebtPaymentService debtPaymentService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPaymentShouldDecrementRemainingAmountAtomicallyAndSavePayment() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        debt.setName("Tarjeta Visa");
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(200), LocalDate.of(2026, 6, 1), "Abono mensual");
        DebtPayment mappedPayment = new DebtPayment();
        DebtPayment savedPayment = new DebtPayment();
        savedPayment.setId(5L);
        DebtPaymentResponse response = new DebtPaymentResponse(5L, 10L, BigDecimal.valueOf(200), LocalDate.of(2026, 6, 1), "Abono mensual", null, null);

        Expense savedExpense = new Expense();
        savedExpense.setId(77L);

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtRepository.decrementRemainingAmount(10L, BigDecimal.valueOf(200))).thenReturn(1);
        when(debtPaymentMapper.toEntity(request)).thenReturn(mappedPayment);
        when(debtPaymentRepository.save(mappedPayment)).thenReturn(savedPayment);
        when(debtPaymentMapper.toResponse(savedPayment)).thenReturn(response);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        DebtPaymentResponse createdPayment = debtPaymentService.createPayment(10L, request);

        Assertions.assertEquals(5L, createdPayment.id());
        Assertions.assertEquals(debt, mappedPayment.getDebt());
        Assertions.assertEquals(LocalDate.of(2026, 6, 1), mappedPayment.getPaymentDate());
        verify(debtRepository).decrementRemainingAmount(10L, BigDecimal.valueOf(200));
        verify(debtPaymentRepository).save(mappedPayment);

        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        Expense capturedExpense = expenseCaptor.getValue();
        Assertions.assertEquals("Abono a deuda: Tarjeta Visa", capturedExpense.getDescription());
        Assertions.assertEquals(BigDecimal.valueOf(200), capturedExpense.getAmount());
        Assertions.assertEquals(LocalDate.of(2026, 6, 1), capturedExpense.getDate());
        Assertions.assertEquals(PaymentMethodType.OTHER, capturedExpense.getPaymentMethod());
        Assertions.assertNull(capturedExpense.getCategory());
        Assertions.assertEquals(savedPayment, capturedExpense.getDebtPayment());

        Assertions.assertEquals(77L, createdPayment.expenseId());
    }

    @Test
    void createPaymentShouldDefaultPaymentDateToTodayWhenOmitted() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(100), null, null);
        DebtPayment mappedPayment = new DebtPayment();

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtRepository.decrementRemainingAmount(10L, BigDecimal.valueOf(100))).thenReturn(1);
        when(debtPaymentMapper.toEntity(request)).thenReturn(mappedPayment);
        when(debtPaymentRepository.save(mappedPayment)).thenReturn(mappedPayment);
        when(debtPaymentMapper.toResponse(mappedPayment)).thenReturn(
                new DebtPaymentResponse(1L, 10L, BigDecimal.valueOf(100), LocalDate.now(), null, null, null)
        );
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(any(Expense.class))).thenReturn(new Expense());

        debtPaymentService.createPayment(10L, request);

        Assertions.assertEquals(LocalDate.now(), mappedPayment.getPaymentDate());
    }

    @Test
    void createPaymentShouldThrowWhenAmountExceedsRemainingAmountOnFastPathCheck() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(150));
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(200), LocalDate.now(), null);

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> debtPaymentService.createPayment(10L, request)
        );
        Assertions.assertEquals("El abono no puede superar el saldo restante de la deuda", exception.getMessage());
        // Rejected on the cheap fast-path check, so the atomic UPDATE (the real guard) is
        // never even attempted, and nothing is persisted.
        verify(debtRepository, never()).decrementRemainingAmount(any(), any());
        verifyNoInteractions(debtPaymentRepository);
    }

    @Test
    void createPaymentShouldRejectAndNotPersistWhenConcurrentDecrementLosesRace() {
        // Regression test for the lost-update race: the in-memory debt.getRemainingAmount()
        // looks sufficient (fast-path check passes), but a concurrent payment already consumed
        // the balance in the database, so the atomic UPDATE affects zero rows. The service must
        // reject the payment and must NOT persist a DebtPayment in that case.
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(200), LocalDate.now(), null);

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtRepository.decrementRemainingAmount(10L, BigDecimal.valueOf(200))).thenReturn(0);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> debtPaymentService.createPayment(10L, request)
        );
        Assertions.assertEquals("El abono no puede superar el saldo restante de la deuda", exception.getMessage());
        verify(debtRepository).decrementRemainingAmount(10L, BigDecimal.valueOf(200));
        verifyNoInteractions(debtPaymentRepository);
    }

    @Test
    void createPaymentShouldAllowAmountEqualToRemainingAmount() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(150));
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(150), LocalDate.now(), null);
        DebtPayment mappedPayment = new DebtPayment();

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtRepository.decrementRemainingAmount(10L, BigDecimal.valueOf(150))).thenReturn(1);
        when(debtPaymentMapper.toEntity(request)).thenReturn(mappedPayment);
        when(debtPaymentRepository.save(mappedPayment)).thenReturn(mappedPayment);
        when(debtPaymentMapper.toResponse(mappedPayment)).thenReturn(
                new DebtPaymentResponse(1L, 10L, BigDecimal.valueOf(150), LocalDate.now(), null, null, null)
        );
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(any(Expense.class))).thenReturn(new Expense());

        debtPaymentService.createPayment(10L, request);

        verify(debtRepository).decrementRemainingAmount(10L, BigDecimal.valueOf(150));
        verify(debtPaymentRepository).save(mappedPayment);
    }

    @Test
    void createPaymentShouldThrowNotFoundWhenDebtBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(50), null, null);
        when(debtRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> debtPaymentService.createPayment(99L, request));
    }

    @Test
    void getPaymentsShouldThrowNotFoundWhenDebtBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        Pageable pageable = PageRequest.of(0, 20);
        when(debtRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> debtPaymentService.getPayments(99L, pageable));
    }

    @Test
    void getPaymentsShouldReturnPagedPaymentsForOwnedDebt() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        Pageable pageable = PageRequest.of(0, 20);
        DebtPayment payment = new DebtPayment();
        Page<DebtPayment> page = new PageImpl<>(List.of(payment), pageable, 1);
        DebtPaymentResponse response = new DebtPaymentResponse(1L, 10L, BigDecimal.valueOf(100), LocalDate.now(), null, null, null);

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtPaymentRepository.findAllByDebt_IdOrderByPaymentDateDescIdDesc(10L, pageable)).thenReturn(page);
        when(debtPaymentMapper.toResponse(payment)).thenReturn(response);

        Page<DebtPaymentResponse> result = debtPaymentService.getPayments(10L, pageable);

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

    private Debt buildDebt(Long debtId, Long userId, BigDecimal totalAmount, BigDecimal remainingAmount) {
        Debt debt = new Debt();
        debt.setId(debtId);
        User user = new User();
        user.setId(userId);
        debt.setUser(user);
        debt.setTotalAmount(totalAmount);
        debt.setRemainingAmount(remainingAmount);
        return debt;
    }
}
