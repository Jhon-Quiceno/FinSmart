package com.smartfinance.backend.tarjetas.service;

import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.tarjetas.mapper.CardMovementMapper;
import com.smartfinance.backend.tarjetas.mapper.InstallmentMapper;
import com.smartfinance.backend.tarjetas.model.dto.CardMovementResponse;
import com.smartfinance.backend.tarjetas.model.dto.CardPaymentRequest;
import com.smartfinance.backend.tarjetas.model.dto.CardPurchaseRequest;
import com.smartfinance.backend.tarjetas.model.dto.InstallmentResponse;
import com.smartfinance.backend.tarjetas.model.entity.CardFranchise;
import com.smartfinance.backend.tarjetas.model.entity.CardMovement;
import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.model.entity.Installment;
import com.smartfinance.backend.tarjetas.model.entity.InstallmentPlan;
import com.smartfinance.backend.tarjetas.model.entity.InstallmentStatus;
import com.smartfinance.backend.tarjetas.repository.CardMovementRepository;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
import com.smartfinance.backend.tarjetas.repository.InstallmentPlanRepository;
import com.smartfinance.backend.tarjetas.repository.InstallmentRepository;
import com.smartfinance.backend.usuario.model.entity.User;
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
class CardMovementServiceTest {

    @Mock
    private CardMovementRepository cardMovementRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMovementMapper cardMovementMapper;

    @Mock
    private AmortizationService amortizationService;

    @Mock
    private InstallmentPlanRepository installmentPlanRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private InstallmentMapper installmentMapper;

    @InjectMocks
    private CardMovementService cardMovementService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerPurchaseShouldIncrementBalanceAtomicallyAndCreateLinkedExpense() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(200_000));
        card.setName("Tarjeta Visa");
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(50_000), LocalDate.of(2026, 6, 1), "Mercado", null);

        CardMovement mappedMovement = new CardMovement();
        CardMovement savedMovement = new CardMovement();
        savedMovement.setId(5L);
        CardMovementResponse mappedResponse = new CardMovementResponse(
                5L, 10L, CardMovementType.PURCHASE, BigDecimal.valueOf(50_000), LocalDate.of(2026, 6, 1), "Mercado", null, null, null, null
        );

        Expense savedExpense = new Expense();
        savedExpense.setId(77L);

        CreditCard cardAfter = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(250_000));

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(creditCardRepository.incrementBalanceWithinLimit(10L, BigDecimal.valueOf(50_000))).thenReturn(1);
        when(cardMovementMapper.toEntity(request)).thenReturn(mappedMovement);
        when(cardMovementRepository.save(mappedMovement)).thenReturn(savedMovement);
        when(cardMovementMapper.toResponse(savedMovement)).thenReturn(mappedResponse);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);
        when(creditCardRepository.findById(10L)).thenReturn(Optional.of(cardAfter));

        CardMovementResponse response = cardMovementService.registerPurchase(10L, request);

        Assertions.assertEquals(5L, response.id());
        Assertions.assertEquals(card, mappedMovement.getCard());
        Assertions.assertEquals(CardMovementType.PURCHASE, mappedMovement.getType());
        Assertions.assertEquals(LocalDate.of(2026, 6, 1), mappedMovement.getDate());
        verify(creditCardRepository).incrementBalanceWithinLimit(10L, BigDecimal.valueOf(50_000));
        verify(cardMovementRepository).save(mappedMovement);

        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        Expense capturedExpense = expenseCaptor.getValue();
        Assertions.assertEquals("Compra con tarjeta: Tarjeta Visa", capturedExpense.getDescription());
        Assertions.assertEquals(BigDecimal.valueOf(50_000), capturedExpense.getAmount());
        Assertions.assertEquals(LocalDate.of(2026, 6, 1), capturedExpense.getDate());
        Assertions.assertEquals(PaymentMethodType.OTHER, capturedExpense.getPaymentMethod());
        Assertions.assertNull(capturedExpense.getCategory());
        Assertions.assertEquals(savedMovement, capturedExpense.getCardMovement());

        Assertions.assertEquals(77L, response.expenseId());
        Assertions.assertNull(response.installmentPlanId());
        Assertions.assertEquals(BigDecimal.valueOf(250_000), response.cardBalanceAfter());
    }

    @Test
    void registerPurchaseShouldDefaultInstallmentCountOneToSimplePurchase() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.ZERO);
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(10_000), null, null, 1);
        CardMovement mappedMovement = new CardMovement();

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(creditCardRepository.incrementBalanceWithinLimit(10L, BigDecimal.valueOf(10_000))).thenReturn(1);
        when(cardMovementMapper.toEntity(request)).thenReturn(mappedMovement);
        when(cardMovementRepository.save(mappedMovement)).thenReturn(mappedMovement);
        when(cardMovementMapper.toResponse(mappedMovement)).thenReturn(
                new CardMovementResponse(1L, 10L, CardMovementType.PURCHASE, BigDecimal.valueOf(10_000), LocalDate.now(), null, null, null, null, null)
        );
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(any(Expense.class))).thenReturn(new Expense());
        when(creditCardRepository.findById(10L)).thenReturn(Optional.of(card));

        cardMovementService.registerPurchase(10L, request);

        Assertions.assertEquals(CardMovementType.PURCHASE, mappedMovement.getType());
        verify(creditCardRepository).incrementBalanceWithinLimit(10L, BigDecimal.valueOf(10_000));
    }

    @Test
    void registerPurchaseShouldCreateInstallmentPlanWithFrozenRateForTwoOrMoreInstallments() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(2_000_000), BigDecimal.ZERO);
        card.setName("Tarjeta Visa");
        card.setMonthlyRate(new BigDecimal("0.021"));
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(700_000), LocalDate.of(2026, 6, 1), "TV", 3);

        List<Installment> schedule = List.of(
                buildInstallment(1, new BigDecimal("233333.33"), new BigDecimal("14700.00")),
                buildInstallment(2, new BigDecimal("233333.33"), new BigDecimal("9800.00")),
                buildInstallment(3, new BigDecimal("233333.34"), new BigDecimal("4900.00"))
        );

        CardMovement mappedMovement = new CardMovement();
        CardMovement savedMovement = new CardMovement();
        savedMovement.setId(6L);
        CardMovementResponse mappedResponse = new CardMovementResponse(
                6L, 10L, CardMovementType.INSTALLMENT_PURCHASE, BigDecimal.valueOf(700_000), LocalDate.of(2026, 6, 1), "TV", null, null, null, null
        );

        InstallmentPlan savedPlan = new InstallmentPlan();
        savedPlan.setId(42L);

        Expense savedExpense = new Expense();
        savedExpense.setId(88L);

        CreditCard cardAfter = buildCard(10L, 1L, BigDecimal.valueOf(2_000_000), BigDecimal.valueOf(700_000));

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(amortizationService.buildSchedule(BigDecimal.valueOf(700_000), 3, new BigDecimal("0.021"), LocalDate.of(2026, 6, 1), card))
                .thenReturn(schedule);
        when(creditCardRepository.incrementBalanceWithinLimit(10L, BigDecimal.valueOf(700_000))).thenReturn(1);
        when(cardMovementMapper.toEntity(request)).thenReturn(mappedMovement);
        when(cardMovementRepository.save(mappedMovement)).thenReturn(savedMovement);
        when(cardMovementMapper.toResponse(savedMovement)).thenReturn(mappedResponse);
        when(installmentPlanRepository.save(any(InstallmentPlan.class))).thenReturn(savedPlan);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);
        when(creditCardRepository.findById(10L)).thenReturn(Optional.of(cardAfter));

        CardMovementResponse response = cardMovementService.registerPurchase(10L, request);

        Assertions.assertEquals(CardMovementType.INSTALLMENT_PURCHASE, mappedMovement.getType());
        Assertions.assertEquals(42L, response.installmentPlanId());
        Assertions.assertEquals(88L, response.expenseId());
        Assertions.assertEquals(BigDecimal.valueOf(700_000), response.cardBalanceAfter());

        ArgumentCaptor<InstallmentPlan> planCaptor = ArgumentCaptor.forClass(InstallmentPlan.class);
        verify(installmentPlanRepository).save(planCaptor.capture());
        InstallmentPlan capturedPlan = planCaptor.getValue();
        Assertions.assertEquals(savedMovement, capturedPlan.getMovement());
        Assertions.assertEquals(3, capturedPlan.getInstallmentCount());
        // Tasa congelada: copia el valor vigente de card.monthlyRate AL MOMENTO de la compra.
        Assertions.assertEquals(new BigDecimal("0.021"), capturedPlan.getRateAtPurchase());
        Assertions.assertEquals(schedule, capturedPlan.getInstallments());
        schedule.forEach(installment -> Assertions.assertEquals(capturedPlan, installment.getPlan()));

        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        // El Expense se crea por el monto TOTAL de la compra, igual que una compra de 1 cuota.
        Assertions.assertEquals(BigDecimal.valueOf(700_000), expenseCaptor.getValue().getAmount());
    }

    @Test
    void registerPurchaseShouldKeepRateAtPurchaseFrozenAfterCardRateChangesLater() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(2_000_000), BigDecimal.ZERO);
        card.setMonthlyRate(new BigDecimal("0.021"));
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(300_000), LocalDate.of(2026, 6, 1), null, 2);
        List<Installment> schedule = List.of(
                buildInstallment(1, new BigDecimal("150000.00"), new BigDecimal("6300.00")),
                buildInstallment(2, new BigDecimal("150000.00"), new BigDecimal("3150.00"))
        );

        CardMovement mappedMovement = new CardMovement();
        CardMovement savedMovement = new CardMovement();
        savedMovement.setId(7L);

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(amortizationService.buildSchedule(BigDecimal.valueOf(300_000), 2, new BigDecimal("0.021"), LocalDate.of(2026, 6, 1), card))
                .thenReturn(schedule);
        when(creditCardRepository.incrementBalanceWithinLimit(10L, BigDecimal.valueOf(300_000))).thenReturn(1);
        when(cardMovementMapper.toEntity(request)).thenReturn(mappedMovement);
        when(cardMovementRepository.save(mappedMovement)).thenReturn(savedMovement);
        when(cardMovementMapper.toResponse(savedMovement)).thenReturn(
                new CardMovementResponse(7L, 10L, CardMovementType.INSTALLMENT_PURCHASE, BigDecimal.valueOf(300_000), LocalDate.of(2026, 6, 1), null, null, null, null, null)
        );
        when(installmentPlanRepository.save(any(InstallmentPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(expenseRepository.save(any(Expense.class))).thenReturn(new Expense());
        when(creditCardRepository.findById(10L)).thenReturn(Optional.of(card));

        cardMovementService.registerPurchase(10L, request);

        // La tasa de la tarjeta cambia DESPUÉS de la compra; el plan ya persistido no se toca.
        card.setMonthlyRate(new BigDecimal("0.035"));

        ArgumentCaptor<InstallmentPlan> planCaptor = ArgumentCaptor.forClass(InstallmentPlan.class);
        verify(installmentPlanRepository).save(planCaptor.capture());
        Assertions.assertEquals(new BigDecimal("0.021"), planCaptor.getValue().getRateAtPurchase());
    }

    @Test
    void registerPurchaseShouldRejectAndNotPersistWhenAmortizationRejectsAmountTooLowForInstallmentCount() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.ZERO);
        CardPurchaseRequest request = new CardPurchaseRequest(new BigDecimal("0.35"), LocalDate.of(2026, 6, 1), null, 48);

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(amortizationService.buildSchedule(new BigDecimal("0.35"), 48, card.getMonthlyRate(), LocalDate.of(2026, 6, 1), card))
                .thenThrow(new IllegalArgumentException("El monto de la compra es demasiado bajo para la cantidad de cuotas seleccionada"));

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> cardMovementService.registerPurchase(10L, request)
        );
        Assertions.assertEquals("El monto de la compra es demasiado bajo para la cantidad de cuotas seleccionada", exception.getMessage());
        // Rechazada ANTES de tocar el saldo o persistir nada: el resguardo anti-capital-negativo
        // corre antes que el guard de cupo.
        verify(creditCardRepository, never()).incrementBalanceWithinLimit(any(), any());
        verifyNoInteractions(cardMovementRepository, expenseRepository, installmentPlanRepository);
    }

    @Test
    void registerPurchaseShouldRejectAndNotPersistWhenOverLimit() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(100_000), BigDecimal.valueOf(90_000));
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(50_000), null, null, null);

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(creditCardRepository.incrementBalanceWithinLimit(10L, BigDecimal.valueOf(50_000))).thenReturn(0);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> cardMovementService.registerPurchase(10L, request)
        );
        Assertions.assertEquals("La compra supera el cupo disponible de la tarjeta", exception.getMessage());
        verify(creditCardRepository).incrementBalanceWithinLimit(10L, BigDecimal.valueOf(50_000));
        verifyNoInteractions(cardMovementRepository, expenseRepository);
    }

    @Test
    void registerPurchaseShouldThrowNotFoundWhenCardBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(50), null, null, null);
        when(creditCardRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> cardMovementService.registerPurchase(99L, request));
        verifyNoInteractions(cardMovementRepository, expenseRepository);
    }

    @Test
    void registerPaymentShouldDecrementBalanceAtomicallyAndNotCreateExpense() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(200_000));
        CardPaymentRequest request = new CardPaymentRequest(BigDecimal.valueOf(80_000), LocalDate.of(2026, 6, 1), "Pago mensual");

        CardMovement mappedMovement = new CardMovement();
        CardMovement savedMovement = new CardMovement();
        savedMovement.setId(9L);
        CardMovementResponse mappedResponse = new CardMovementResponse(
                9L, 10L, CardMovementType.PAYMENT, BigDecimal.valueOf(80_000), LocalDate.of(2026, 6, 1), "Pago mensual", null, null, null, null
        );
        CreditCard cardAfter = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(120_000));

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(creditCardRepository.decrementBalance(10L, BigDecimal.valueOf(80_000))).thenReturn(1);
        when(cardMovementMapper.toEntity(request)).thenReturn(mappedMovement);
        when(cardMovementRepository.save(mappedMovement)).thenReturn(savedMovement);
        when(cardMovementMapper.toResponse(savedMovement)).thenReturn(mappedResponse);
        when(creditCardRepository.findById(10L)).thenReturn(Optional.of(cardAfter));

        CardMovementResponse response = cardMovementService.registerPayment(10L, request);

        Assertions.assertEquals(9L, response.id());
        Assertions.assertEquals(CardMovementType.PAYMENT, mappedMovement.getType());
        Assertions.assertEquals(LocalDate.of(2026, 6, 1), mappedMovement.getDate());
        Assertions.assertNull(response.expenseId());
        Assertions.assertEquals(BigDecimal.valueOf(120_000), response.cardBalanceAfter());
        verify(creditCardRepository).decrementBalance(10L, BigDecimal.valueOf(80_000));
        verify(cardMovementRepository).save(mappedMovement);
        verifyNoInteractions(expenseRepository, userRepository);
    }

    @Test
    void registerPaymentShouldRejectAndNotPersistWhenAmountExceedsCurrentBalance() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(50_000));
        CardPaymentRequest request = new CardPaymentRequest(BigDecimal.valueOf(80_000), null, null);

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(creditCardRepository.decrementBalance(10L, BigDecimal.valueOf(80_000))).thenReturn(0);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> cardMovementService.registerPayment(10L, request)
        );
        Assertions.assertEquals("El pago no puede superar el saldo actual de la tarjeta", exception.getMessage());
        verify(creditCardRepository).decrementBalance(10L, BigDecimal.valueOf(80_000));
        verifyNoInteractions(cardMovementRepository);
    }

    @Test
    void registerPaymentShouldThrowNotFoundWhenCardBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        CardPaymentRequest request = new CardPaymentRequest(BigDecimal.valueOf(50), null, null);
        when(creditCardRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> cardMovementService.registerPayment(99L, request));
        verifyNoInteractions(cardMovementRepository);
    }

    @Test
    void getMovementsShouldThrowNotFoundWhenCardBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        Pageable pageable = PageRequest.of(0, 20);
        when(creditCardRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> cardMovementService.getMovements(99L, null, pageable));
    }

    @Test
    void getMovementsShouldReturnPagedMovementsForOwnedCard() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(200_000));
        Pageable pageable = PageRequest.of(0, 20);
        CardMovement movement = new CardMovement();
        Page<CardMovement> page = new PageImpl<>(List.of(movement), pageable, 1);
        CardMovementResponse response = new CardMovementResponse(
                1L, 10L, CardMovementType.PURCHASE, BigDecimal.valueOf(100), LocalDate.now(), null, null, null, null, null
        );

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(cardMovementRepository.findAllByCard_Id(10L, pageable)).thenReturn(page);
        when(cardMovementMapper.toResponse(movement)).thenReturn(response);

        Page<CardMovementResponse> result = cardMovementService.getMovements(10L, null, pageable);

        Assertions.assertEquals(1, result.getTotalElements());
        verify(cardMovementRepository, never()).findAllByCard_IdAndType(any(), any(), any());
    }

    @Test
    void getMovementsShouldFilterByTypeWhenProvided() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(200_000));
        Pageable pageable = PageRequest.of(0, 20);
        Page<CardMovement> page = new PageImpl<>(List.of(), pageable, 0);

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(cardMovementRepository.findAllByCard_IdAndType(10L, CardMovementType.PAYMENT, pageable)).thenReturn(page);

        cardMovementService.getMovements(10L, CardMovementType.PAYMENT, pageable);

        verify(cardMovementRepository).findAllByCard_IdAndType(10L, CardMovementType.PAYMENT, pageable);
        verify(cardMovementRepository, never()).findAllByCard_Id(any(), any());
    }

    @Test
    void getInstallmentsShouldReturnScheduleOrderedByNumberForOwnedCard() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(2_000_000), BigDecimal.valueOf(700_000));
        CardMovement movement = new CardMovement();
        movement.setCard(card);
        InstallmentPlan plan = new InstallmentPlan();
        plan.setId(42L);
        plan.setMovement(movement);

        Installment installment1 = buildInstallment(1, new BigDecimal("233333.33"), new BigDecimal("14700.00"));
        Installment installment2 = buildInstallment(2, new BigDecimal("233333.33"), new BigDecimal("9800.00"));

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(installmentPlanRepository.findByMovement_Id(6L)).thenReturn(Optional.of(plan));
        when(installmentRepository.findAllByPlan_IdOrderByNumber(42L)).thenReturn(List.of(installment1, installment2));
        when(installmentMapper.toResponse(installment1)).thenReturn(
                new InstallmentResponse(1L, 1, installment1.getCapitalAmount(), installment1.getInterestAmount(), installment1.getDueDate(), InstallmentStatus.PENDING)
        );
        when(installmentMapper.toResponse(installment2)).thenReturn(
                new InstallmentResponse(2L, 2, installment2.getCapitalAmount(), installment2.getInterestAmount(), installment2.getDueDate(), InstallmentStatus.PENDING)
        );

        List<InstallmentResponse> result = cardMovementService.getInstallments(10L, 6L);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(1, result.get(0).number());
        Assertions.assertEquals(2, result.get(1).number());
    }

    @Test
    void getInstallmentsShouldThrowNotFoundWhenCardBelongsToAnotherUser() {
        setAuthenticatedUser(1L);
        when(creditCardRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> cardMovementService.getInstallments(99L, 6L));
        verifyNoInteractions(installmentPlanRepository, installmentRepository);
    }

    @Test
    void getInstallmentsShouldThrowNotFoundWhenMovementHasNoPlan() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(2_000_000), BigDecimal.valueOf(700_000));

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(installmentPlanRepository.findByMovement_Id(6L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> cardMovementService.getInstallments(10L, 6L));
        verifyNoInteractions(installmentRepository);
    }

    @Test
    void getInstallmentsShouldThrowNotFoundWhenPlanBelongsToAnotherCard() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(2_000_000), BigDecimal.valueOf(700_000));
        CreditCard otherCard = buildCard(20L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.ZERO);
        CardMovement otherMovement = new CardMovement();
        otherMovement.setCard(otherCard);
        InstallmentPlan planOfOtherCard = new InstallmentPlan();
        planOfOtherCard.setId(43L);
        planOfOtherCard.setMovement(otherMovement);

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));
        when(installmentPlanRepository.findByMovement_Id(6L)).thenReturn(Optional.of(planOfOtherCard));

        Assertions.assertThrows(ResourceNotFoundException.class, () -> cardMovementService.getInstallments(10L, 6L));
        verifyNoInteractions(installmentRepository);
    }

    private Installment buildInstallment(int number, BigDecimal capital, BigDecimal interest) {
        Installment installment = new Installment();
        installment.setNumber(number);
        installment.setCapitalAmount(capital);
        installment.setInterestAmount(interest);
        installment.setDueDate(LocalDate.of(2026, 6 + number - 1, 15));
        installment.setStatus(InstallmentStatus.PENDING);
        return installment;
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

    private CreditCard buildCard(Long cardId, Long userId, BigDecimal creditLimit, BigDecimal currentBalance) {
        CreditCard card = new CreditCard();
        card.setId(cardId);
        card.setUser(buildUser(userId));
        card.setName("Tarjeta Visa");
        card.setFranchise(CardFranchise.VISA);
        card.setCreditLimit(creditLimit);
        card.setMonthlyRate(BigDecimal.valueOf(0.025));
        card.setCutoffDay(5);
        card.setPaymentDueDay(20);
        card.setCurrentBalance(currentBalance);
        return card;
    }
}
