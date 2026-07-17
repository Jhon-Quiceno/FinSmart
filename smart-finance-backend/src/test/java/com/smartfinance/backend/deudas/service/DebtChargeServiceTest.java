package com.smartfinance.backend.deudas.service;

import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.deudas.mapper.DebtChargeMapper;
import com.smartfinance.backend.deudas.mapper.DebtMapper;
import com.smartfinance.backend.deudas.model.dto.DebtChargeRequest;
import com.smartfinance.backend.deudas.model.dto.DebtChargeResponse;
import com.smartfinance.backend.deudas.model.dto.DebtResponse;
import com.smartfinance.backend.deudas.model.entity.Debt;
import com.smartfinance.backend.deudas.model.entity.DebtCharge;
import com.smartfinance.backend.deudas.repository.DebtChargeRepository;
import com.smartfinance.backend.deudas.repository.DebtRepository;
import com.smartfinance.backend.usuario.model.entity.User;
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtChargeServiceTest {

    @Mock
    private DebtChargeRepository debtChargeRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private DebtChargeMapper debtChargeMapper;

    @Mock
    private DebtMapper debtMapper;

    @InjectMocks
    private DebtChargeService debtChargeService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createChargeShouldIncrementRemainingAmountAtomicallyAndSaveCharge() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        Debt updatedDebt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(800));
        DebtChargeRequest request = new DebtChargeRequest(BigDecimal.valueOf(200), LocalDate.of(2026, 6, 1), "Compra supermercado");
        DebtCharge mappedCharge = new DebtCharge();
        DebtCharge savedCharge = new DebtCharge();
        savedCharge.setId(5L);
        DebtResponse response = new DebtResponse(10L, "Tarjeta Visa", BigDecimal.valueOf(1000), BigDecimal.valueOf(800), null, null, null, null);

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtRepository.incrementRemainingAmount(10L, BigDecimal.valueOf(200))).thenReturn(1);
        when(debtRepository.findById(10L)).thenReturn(Optional.of(updatedDebt));
        when(debtChargeMapper.toEntity(request)).thenReturn(mappedCharge);
        when(debtChargeRepository.save(mappedCharge)).thenReturn(savedCharge);
        when(debtMapper.toResponse(updatedDebt)).thenReturn(response);

        DebtResponse result = debtChargeService.createCharge(10L, request);

        Assertions.assertEquals(response, result);
        Assertions.assertEquals(updatedDebt, mappedCharge.getDebt());
        Assertions.assertEquals(LocalDate.of(2026, 6, 1), mappedCharge.getChargeDate());
        verify(debtRepository).incrementRemainingAmount(10L, BigDecimal.valueOf(200));
        verify(debtChargeRepository).save(mappedCharge);
    }

    @Test
    void createChargeShouldDefaultChargeDateToTodayWhenOmitted() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        Debt updatedDebt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(700));
        DebtChargeRequest request = new DebtChargeRequest(BigDecimal.valueOf(100), null, null);
        DebtCharge mappedCharge = new DebtCharge();

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtRepository.incrementRemainingAmount(10L, BigDecimal.valueOf(100))).thenReturn(1);
        when(debtRepository.findById(10L)).thenReturn(Optional.of(updatedDebt));
        when(debtChargeMapper.toEntity(request)).thenReturn(mappedCharge);
        when(debtChargeRepository.save(mappedCharge)).thenReturn(mappedCharge);
        when(debtMapper.toResponse(updatedDebt)).thenReturn(
                new DebtResponse(10L, "Tarjeta Visa", BigDecimal.valueOf(1000), BigDecimal.valueOf(700), null, null, null, null)
        );

        debtChargeService.createCharge(10L, request);

        Assertions.assertEquals(LocalDate.now(), mappedCharge.getChargeDate());
    }

    @Test
    void createChargeShouldThrowNotFoundWhenDebtBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        DebtChargeRequest request = new DebtChargeRequest(BigDecimal.valueOf(50), null, null);
        when(debtRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> debtChargeService.createCharge(99L, request));
        verify(debtRepository, never()).incrementRemainingAmount(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(debtChargeRepository);
    }

    @Test
    void createChargeShouldThrowNotFoundWhenDebtDeletedConcurrently() {
        // Regression coverage for the race between the ownership check and the atomic UPDATE:
        // if the debt is removed by another transaction in between, incrementRemainingAmount
        // affects zero rows and the service must reject without persisting a DebtCharge.
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        DebtChargeRequest request = new DebtChargeRequest(BigDecimal.valueOf(200), LocalDate.now(), null);

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtRepository.incrementRemainingAmount(10L, BigDecimal.valueOf(200))).thenReturn(0);

        Assertions.assertThrows(ResourceNotFoundException.class, () -> debtChargeService.createCharge(10L, request));
        verify(debtRepository).incrementRemainingAmount(10L, BigDecimal.valueOf(200));
        verifyNoInteractions(debtChargeRepository);
    }

    @Test
    void getChargesShouldThrowNotFoundWhenDebtBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        Pageable pageable = PageRequest.of(0, 20);
        when(debtRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> debtChargeService.getCharges(99L, pageable));
    }

    @Test
    void getChargesShouldReturnPagedChargesForOwnedDebt() {
        setAuthenticatedUser(1L);
        Debt debt = buildDebt(10L, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(600));
        Pageable pageable = PageRequest.of(0, 20);
        DebtCharge charge = new DebtCharge();
        Page<DebtCharge> page = new PageImpl<>(List.of(charge), pageable, 1);
        DebtChargeResponse response = new DebtChargeResponse(1L, 10L, BigDecimal.valueOf(100), LocalDate.now(), null, null);

        when(debtRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(debt));
        when(debtChargeRepository.findAllByDebt_IdOrderByChargeDateDescIdDesc(10L, pageable)).thenReturn(page);
        when(debtChargeMapper.toResponse(charge)).thenReturn(response);

        Page<DebtChargeResponse> result = debtChargeService.getCharges(10L, pageable);

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
        debt.setName("Tarjeta Visa");
        debt.setTotalAmount(totalAmount);
        debt.setRemainingAmount(remainingAmount);
        return debt;
    }
}
