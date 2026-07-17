package com.smartfinance.backend.tarjetas.service;

import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.servicios.service.notification.NotificationDispatcher;
import com.smartfinance.backend.tarjetas.model.entity.CardMovement;
import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.model.entity.Installment;
import com.smartfinance.backend.tarjetas.model.entity.InstallmentStatus;
import com.smartfinance.backend.tarjetas.repository.CardMovementRepository;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
import com.smartfinance.backend.tarjetas.repository.InstallmentRepository;
import com.smartfinance.backend.usuario.model.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CycleCloseServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T06:00:00Z"), ZoneOffset.UTC
    );

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private CardMovementRepository cardMovementRepository;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    private CycleCloseService cycleCloseService;

    @Test
    void closeCycleShouldAggregateInterestCreateMovementAndBillPendingInstallments() {
        cycleCloseService = new CycleCloseService(
                creditCardRepository, installmentRepository, cardMovementRepository, notificationDispatcher, FIXED_CLOCK
        );
        CreditCard card = buildCard(1L, 10L, "Tarjeta Visa", BigDecimal.valueOf(500_000));
        CreditCard cardAfter = buildCard(1L, 10L, "Tarjeta Visa", BigDecimal.valueOf(514_700));

        Installment installment1 = buildInstallment(100L, BigDecimal.valueOf(14_700));
        Installment installment2 = buildInstallment(101L, BigDecimal.valueOf(9_800));

        when(creditCardRepository.markCutoffClosed(1L, LocalDate.of(2026, 7, 15))).thenReturn(1);
        when(installmentRepository.findByPlan_Movement_Card_IdAndStatusAndDueDateLessThanEqual(
                1L, InstallmentStatus.PENDING, LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of(installment1, installment2));
        when(creditCardRepository.findById(1L)).thenReturn(Optional.of(card), Optional.of(cardAfter));
        CardMovement savedMovement = new CardMovement();
        savedMovement.setId(999L);
        when(cardMovementRepository.save(any(CardMovement.class))).thenReturn(savedMovement);

        cycleCloseService.closeCycle(1L);

        ArgumentCaptor<CardMovement> movementCaptor = ArgumentCaptor.forClass(CardMovement.class);
        verify(cardMovementRepository).save(movementCaptor.capture());
        CardMovement createdMovement = movementCaptor.getValue();
        Assertions.assertEquals(CardMovementType.INTEREST, createdMovement.getType());
        Assertions.assertEquals(0, createdMovement.getAmount().compareTo(BigDecimal.valueOf(24_500)));
        Assertions.assertEquals(LocalDate.of(2026, 7, 15), createdMovement.getCycleCloseDate());

        verify(creditCardRepository).incrementBalance(1L, BigDecimal.valueOf(24_500));

        Assertions.assertEquals(InstallmentStatus.BILLED, installment1.getStatus());
        Assertions.assertEquals(InstallmentStatus.BILLED, installment2.getStatus());
        Assertions.assertEquals(savedMovement, installment1.getInterestMovement());
        Assertions.assertEquals(savedMovement, installment2.getInterestMovement());
        verify(installmentRepository).saveAll(anyList());

        verify(notificationDispatcher).dispatch(
                eq(10L), eq(NotificationType.CARD_CYCLE_CLOSE), anyString(), anyString(),
                eq("card-cycle-close:1:2026-07-15")
        );
    }

    @Test
    void closeCycleShouldDoNothingWhenAlreadyClosedTodayIdempotencyGuard() {
        cycleCloseService = new CycleCloseService(
                creditCardRepository, installmentRepository, cardMovementRepository, notificationDispatcher, FIXED_CLOCK
        );
        when(creditCardRepository.markCutoffClosed(1L, LocalDate.of(2026, 7, 15))).thenReturn(0);

        cycleCloseService.closeCycle(1L);

        verify(installmentRepository, never()).findByPlan_Movement_Card_IdAndStatusAndDueDateLessThanEqual(
                any(), any(), any()
        );
        verify(cardMovementRepository, never()).save(any(CardMovement.class));
        verify(creditCardRepository, never()).incrementBalance(any(), any());
        verify(notificationDispatcher, never()).dispatch(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void closeCycleShouldCloseStaleCycleEvenWhenLastCutoffIsOldSimulatingDowntimeCatchUp() {
        cycleCloseService = new CycleCloseService(
                creditCardRepository, installmentRepository, cardMovementRepository, notificationDispatcher, FIXED_CLOCK
        );
        // La tarjeta debía cerrar el 15/06 pero el servidor estuvo caído ese día; lastCutoffDate
        // quedó en 15/05 (dos meses de atraso). El job la vuelve a intentar recién el 15/07: el
        // guard de idempotencia sigue viendo lastCutoffDate < closeDate y permite el cierre.
        when(creditCardRepository.markCutoffClosed(1L, LocalDate.of(2026, 7, 15))).thenReturn(1);
        Installment overdueInstallment = buildInstallment(200L, BigDecimal.valueOf(5_000));
        when(installmentRepository.findByPlan_Movement_Card_IdAndStatusAndDueDateLessThanEqual(
                1L, InstallmentStatus.PENDING, LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of(overdueInstallment));
        CreditCard card = buildCard(1L, 10L, "Tarjeta Catch-up", BigDecimal.valueOf(100_000));
        CreditCard cardAfter = buildCard(1L, 10L, "Tarjeta Catch-up", BigDecimal.valueOf(105_000));
        when(creditCardRepository.findById(1L)).thenReturn(Optional.of(card), Optional.of(cardAfter));
        CardMovement savedMovement = new CardMovement();
        savedMovement.setId(500L);
        when(cardMovementRepository.save(any(CardMovement.class))).thenReturn(savedMovement);

        cycleCloseService.closeCycle(1L);

        verify(creditCardRepository).markCutoffClosed(1L, LocalDate.of(2026, 7, 15));
        verify(creditCardRepository).incrementBalance(1L, BigDecimal.valueOf(5_000));
        Assertions.assertEquals(InstallmentStatus.BILLED, overdueInstallment.getStatus());
        verify(notificationDispatcher).dispatch(
                eq(10L), eq(NotificationType.CARD_CYCLE_CLOSE), anyString(), anyString(),
                eq("card-cycle-close:1:2026-07-15")
        );
    }

    @Test
    void closeCycleShouldNotCreateInterestMovementWhenCardHasNoPendingInstallments() {
        cycleCloseService = new CycleCloseService(
                creditCardRepository, installmentRepository, cardMovementRepository, notificationDispatcher, FIXED_CLOCK
        );
        when(creditCardRepository.markCutoffClosed(1L, LocalDate.of(2026, 7, 15))).thenReturn(1);
        when(installmentRepository.findByPlan_Movement_Card_IdAndStatusAndDueDateLessThanEqual(
                1L, InstallmentStatus.PENDING, LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of());

        cycleCloseService.closeCycle(1L);

        verify(cardMovementRepository, never()).save(any(CardMovement.class));
        verify(creditCardRepository, never()).incrementBalance(any(), any());
        verify(notificationDispatcher, never()).dispatch(any(), any(), anyString(), anyString(), anyString());
    }

    private CreditCard buildCard(Long id, Long userId, String name, BigDecimal currentBalance) {
        CreditCard card = new CreditCard();
        card.setId(id);
        card.setUser(buildUser(userId));
        card.setName(name);
        card.setCurrentBalance(currentBalance);
        return card;
    }

    private User buildUser(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private Installment buildInstallment(Long id, BigDecimal interestAmount) {
        Installment installment = new Installment();
        installment.setId(id);
        installment.setInterestAmount(interestAmount);
        installment.setStatus(InstallmentStatus.PENDING);
        return installment;
    }
}
