package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.debt.DebtRequest;
import com.smartfinance.backend.dto.debt.DebtResponse;
import com.smartfinance.backend.dto.debt.DebtUpdateRequest;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.DebtMapper;
import com.smartfinance.backend.model.Debt;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.DebtRepository;
import com.smartfinance.backend.repository.UserRepository;
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
class DebtServiceTest {

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DebtMapper debtMapper;

    @InjectMocks
    private DebtService debtService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDebtShouldSeedRemainingAmountFromTotalAmount() {
        setAuthenticatedUser(1L);
        DebtRequest request = new DebtRequest("Tarjeta de crédito", BigDecimal.valueOf(1000), BigDecimal.valueOf(2.5), null);
        Debt mappedDebt = new Debt();
        Debt savedDebt = new Debt();
        savedDebt.setId(10L);
        DebtResponse response = new DebtResponse(10L, "Tarjeta de crédito", BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000), BigDecimal.valueOf(2.5), null, null, null);

        when(debtMapper.toEntity(request)).thenReturn(mappedDebt);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(debtRepository.save(mappedDebt)).thenReturn(savedDebt);
        when(debtMapper.toResponse(savedDebt)).thenReturn(response);

        DebtResponse createdDebt = debtService.createDebt(request);

        Assertions.assertEquals(10L, createdDebt.id());
        Assertions.assertEquals(BigDecimal.valueOf(1000), mappedDebt.getRemainingAmount());
        Assertions.assertEquals(1L, mappedDebt.getUser().getId());
    }

    @Test
    void updateDebtShouldNotTouchRemainingAmount() {
        setAuthenticatedUser(1L);
        Debt debt = new Debt();
        debt.setId(20L);
        debt.setUser(buildUser(1L));
        debt.setTotalAmount(BigDecimal.valueOf(500));
        debt.setRemainingAmount(BigDecimal.valueOf(300));
        DebtUpdateRequest request = new DebtUpdateRequest("Préstamo actualizado", BigDecimal.valueOf(3.0), LocalDate.of(2026, 12, 1));

        when(debtRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(debt));
        when(debtRepository.save(debt)).thenReturn(debt);
        when(debtMapper.toResponse(debt)).thenReturn(
                new DebtResponse(20L, "Préstamo actualizado", BigDecimal.valueOf(500), BigDecimal.valueOf(300),
                        BigDecimal.valueOf(3.0), LocalDate.of(2026, 12, 1), null, null)
        );

        DebtResponse updatedDebt = debtService.updateDebt(20L, request);

        Assertions.assertEquals(BigDecimal.valueOf(300), updatedDebt.remainingAmount());
        Assertions.assertEquals(BigDecimal.valueOf(500), debt.getTotalAmount());
    }

    @Test
    void updateDebtShouldThrowNotFoundWhenDebtBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        DebtUpdateRequest request = new DebtUpdateRequest("Otro nombre", null, null);
        when(debtRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> debtService.updateDebt(99L, request));
    }

    @Test
    void deleteDebtShouldDeleteWhenOwnedByCurrentUser() {
        setAuthenticatedUser(3L);
        Debt debt = new Debt();
        debt.setId(55L);
        debt.setUser(buildUser(3L));
        when(debtRepository.findByIdAndUser_Id(55L, 3L)).thenReturn(Optional.of(debt));

        debtService.deleteDebt(55L);

        verify(debtRepository).delete(debt);
    }

    @Test
    void deleteDebtShouldThrowNotFoundWhenOwnedByAnotherUser() {
        setAuthenticatedUser(3L);
        when(debtRepository.findByIdAndUser_Id(55L, 3L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> debtService.deleteDebt(55L));
    }

    @Test
    void getDebtsShouldReturnPagedDebtsForCurrentUser() {
        setAuthenticatedUser(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Debt debt = new Debt();
        Page<Debt> page = new PageImpl<>(List.of(debt), pageable, 1);
        DebtResponse response = new DebtResponse(1L, "Deuda", BigDecimal.TEN, BigDecimal.TEN, null, null, null, null);

        when(debtRepository.findAllByUser_Id(1L, pageable)).thenReturn(page);
        when(debtMapper.toResponse(debt)).thenReturn(response);

        Page<DebtResponse> result = debtService.getDebts(pageable);

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
}
