package com.smartfinance.backend.tarjetas.service;

import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.tarjetas.mapper.CardMovementMapper;
import com.smartfinance.backend.tarjetas.model.dto.CardMovementResponse;
import com.smartfinance.backend.tarjetas.model.dto.CardPaymentRequest;
import com.smartfinance.backend.tarjetas.model.dto.CardPurchaseRequest;
import com.smartfinance.backend.tarjetas.model.entity.CardFranchise;
import com.smartfinance.backend.tarjetas.model.entity.CardMovement;
import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.repository.CardMovementRepository;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
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
    void registerPurchaseShouldRejectTwoOrMoreInstallmentsWithClearMessage() {
        setAuthenticatedUser(1L);
        CreditCard card = buildCard(10L, 1L, BigDecimal.valueOf(1_000_000), BigDecimal.ZERO);
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(700_000), null, null, 3);

        when(creditCardRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(card));

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> cardMovementService.registerPurchase(10L, request)
        );
        Assertions.assertEquals("Las compras a cuotas todavía no están disponibles", exception.getMessage());
        // Rechazada antes de tocar el saldo o persistir nada: el diferido a cuotas se habilita
        // en Fase B.3.
        verify(creditCardRepository, never()).incrementBalanceWithinLimit(any(), any());
        verifyNoInteractions(cardMovementRepository, expenseRepository);
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
