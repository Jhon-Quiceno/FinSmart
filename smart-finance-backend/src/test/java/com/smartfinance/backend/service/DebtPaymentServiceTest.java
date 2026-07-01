package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.debt.DebtPaymentRequest;
import com.smartfinance.backend.dto.debt.DebtPaymentResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.DebtPaymentMapper;
import com.smartfinance.backend.model.Debt;
import com.smartfinance.backend.model.DebtPayment;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.DebtPaymentRepository;
import com.smartfinance.backend.repository.DebtRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtPaymentServiceTest {

    @Mock
    private DebtPaymentRepository debtPaymentRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private DebtPaymentMapper debtPaymentMapper;

    @InjectMocks
    private DebtPaymentService debtPaymentService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPaymentShouldDecrementRemainingAmountAndSaveBoth() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(200), LocalDate.of(2026, 6, 1), "Abono mensual");
        DebtPayment mappedPayment = new DebtPayment();
        DebtPayment savedPayment = new DebtPayment();
        savedPayment.setId(5L);
        DebtPaymentResponse response = new DebtPaymentResponse(5L, 10L, BigDecimal.valueOf(200), LocalDate.of(2026, 6, 1), "Abono mensual", null);

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtPaymentMapper.toEntity(request)).thenReturn(mappedPayment);
        when(debtRepository.save(debt)).thenReturn(debt);
        when(debtPaymentRepository.save(mappedPayment)).thenReturn(savedPayment);
        when(debtPaymentMapper.toResponse(savedPayment)).thenReturn(response);

        DebtPaymentResponse createdPayment = debtPaymentService.createPayment(10L, request);

        Assertions.assertEquals(5L, createdPayment.id());
        Assertions.assertEquals(BigDecimal.valueOf(400), debt.getRemainingAmount());
        Assertions.assertEquals(debt, mappedPayment.getDebt());
        Assertions.assertEquals(LocalDate.of(2026, 6, 1), mappedPayment.getPaymentDate());
        verify(debtRepository).save(debt);
    }

    @Test
    void createPaymentShouldDefaultPaymentDateToTodayWhenOmitted() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(100), null, null);
        DebtPayment mappedPayment = new DebtPayment();

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtPaymentMapper.toEntity(request)).thenReturn(mappedPayment);
        when(debtRepository.save(debt)).thenReturn(debt);
        when(debtPaymentRepository.save(mappedPayment)).thenReturn(mappedPayment);
        when(debtPaymentMapper.toResponse(mappedPayment)).thenReturn(
                new DebtPaymentResponse(1L, 10L, BigDecimal.valueOf(100), LocalDate.now(), null, null)
        );

        debtPaymentService.createPayment(10L, request);

        Assertions.assertEquals(LocalDate.now(), mappedPayment.getPaymentDate());
    }

    @Test
    void createPaymentShouldThrowWhenAmountExceedsRemainingAmount() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(150));
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(200), LocalDate.now(), null);

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> debtPaymentService.createPayment(10L, request)
        );
        Assertions.assertEquals("El abono no puede superar el saldo restante de la deuda", exception.getMessage());
        Assertions.assertEquals(BigDecimal.valueOf(150), debt.getRemainingAmount());
    }

    @Test
    void createPaymentShouldAllowAmountEqualToRemainingAmount() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(150));
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(150), LocalDate.now(), null);
        DebtPayment mappedPayment = new DebtPayment();

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtPaymentMapper.toEntity(request)).thenReturn(mappedPayment);
        when(debtRepository.save(debt)).thenReturn(debt);
        when(debtPaymentRepository.save(mappedPayment)).thenReturn(mappedPayment);
        when(debtPaymentMapper.toResponse(mappedPayment)).thenReturn(
                new DebtPaymentResponse(1L, 10L, BigDecimal.valueOf(150), LocalDate.now(), null, null)
        );

        debtPaymentService.createPayment(10L, request);

        Assertions.assertEquals(BigDecimal.ZERO, debt.getRemainingAmount());
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
        DebtPaymentResponse response = new DebtPaymentResponse(1L, 10L, BigDecimal.valueOf(100), LocalDate.now(), null, null);

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
