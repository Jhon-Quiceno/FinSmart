package com.smartfinance.backend.tarjetas.service;

import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.tarjetas.mapper.CardMovementMapper;
import com.smartfinance.backend.tarjetas.model.dto.CardMovementResponse;
import com.smartfinance.backend.tarjetas.model.dto.CardPaymentRequest;
import com.smartfinance.backend.tarjetas.model.dto.CardPurchaseRequest;
import com.smartfinance.backend.tarjetas.model.entity.CardMovement;
import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.repository.CardMovementRepository;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Lógica de negocio para registrar y listar {@link CardMovement} contra una
 * {@link CreditCard} propiedad del usuario actual.
 *
 * <p>{@link #registerPurchase} es el único lugar donde {@link CreditCard#getCurrentBalance()} se
 * incrementa por una compra — valida que la tarjeta pertenezca al usuario actual, luego
 * incrementa el saldo vía {@link CreditCardRepository#incrementBalanceWithinLimit} (un
 * {@code UPDATE} atómico condicionado al cupo, no lectura-y-luego-escritura) antes de persistir
 * el {@link CardMovement}, de modo que dos compras concurrentes contra la misma tarjeta no
 * puedan ambas validar contra el mismo cupo obsoleto y producir un "lost update". Cuando la
 * compra es de una sola cuota (o {@code installmentCount} es {@code null}), también crea un
 * {@link Expense} vinculado (mirror de {@code DebtPaymentService#createPayment}). Compras a 2 o
 * más cuotas se rechazan explícitamente en esta fase — el soporte real de cuotas se construye en
 * Fase B.3 encima de este slice.
 *
 * <p>{@link #registerPayment} es el mirror simétrico para pagos: decrementa el saldo vía
 * {@link CreditCardRepository#decrementBalance} y NO crea un {@link Expense} — un pago de
 * tarjeta no es un gasto nuevo, ya se contabilizó como {@code PURCHASE} en su momento.
 */
@Service
public class CardMovementService {

    private static final String INSTALLMENTS_NOT_AVAILABLE_MESSAGE =
            "Las compras a cuotas todavía no están disponibles";
    private static final String PURCHASE_OVER_LIMIT_MESSAGE =
            "La compra supera el cupo disponible de la tarjeta";
    private static final String PAYMENT_OVER_BALANCE_MESSAGE =
            "El pago no puede superar el saldo actual de la tarjeta";
    private static final String CARD_NOT_FOUND_MESSAGE = "Tarjeta no encontrada";

    private final CardMovementRepository cardMovementRepository;
    private final CreditCardRepository creditCardRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CardMovementMapper cardMovementMapper;

    public CardMovementService(
            CardMovementRepository cardMovementRepository,
            CreditCardRepository creditCardRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            CardMovementMapper cardMovementMapper
    ) {
        this.cardMovementRepository = cardMovementRepository;
        this.creditCardRepository = creditCardRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.cardMovementMapper = cardMovementMapper;
    }

    @Transactional(readOnly = true)
    public Page<CardMovementResponse> getMovements(Long cardId, CardMovementType type, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        findOwnedCard(cardId, userId);

        Page<CardMovement> movements = type != null
                ? cardMovementRepository.findAllByCard_IdAndType(cardId, type, pageable)
                : cardMovementRepository.findAllByCard_Id(cardId, pageable);

        // cardBalanceAfter/expenseId/installmentPlanId quedan null en el listado: el saldo
        // actual de la tarjeta no representa el saldo justo después de un movimiento histórico
        // (mirror del mismo gap ya existente en DebtPaymentResponse#expenseId al listar abonos).
        return movements.map(cardMovementMapper::toResponse);
    }

    @Transactional
    public CardMovementResponse registerPurchase(Long cardId, CardPurchaseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CreditCard card = findOwnedCard(cardId, userId);

        if (request.installmentCount() != null && request.installmentCount() >= 2) {
            // El diferido a cuotas (INSTALLMENT_PURCHASE + AmortizationService) se habilita en
            // Fase B.3; esta fase solo soporta compras de una sola cuota.
            throw new IllegalArgumentException(INSTALLMENTS_NOT_AVAILABLE_MESSAGE);
        }

        int updatedRows = creditCardRepository.incrementBalanceWithinLimit(cardId, request.amount());
        if (updatedRows == 0) {
            // Otra compra concurrente consumió el cupo disponible entre la lectura de la
            // tarjeta y este UPDATE atómico; se rechaza sin persistir el CardMovement.
            throw new IllegalArgumentException(PURCHASE_OVER_LIMIT_MESSAGE);
        }

        LocalDate purchaseDate = request.date() != null ? request.date() : LocalDate.now();

        CardMovement movement = cardMovementMapper.toEntity(request);
        movement.setCard(card);
        movement.setType(CardMovementType.PURCHASE);
        movement.setDate(purchaseDate);
        CardMovement savedMovement = cardMovementRepository.save(movement);

        Expense expense = new Expense();
        expense.setUser(userRepository.getReferenceById(userId));
        expense.setDescription("Compra con tarjeta: " + card.getName());
        expense.setAmount(request.amount());
        expense.setDate(purchaseDate);
        expense.setPaymentMethod(PaymentMethodType.OTHER);
        expense.setCategory(null);
        expense.setCardMovement(savedMovement);
        Expense savedExpense = expenseRepository.save(expense);

        CreditCard cardAfter = findCardById(cardId);
        CardMovementResponse mappedResponse = cardMovementMapper.toResponse(savedMovement);
        return new CardMovementResponse(
                mappedResponse.id(),
                mappedResponse.cardId(),
                mappedResponse.type(),
                mappedResponse.amount(),
                mappedResponse.date(),
                mappedResponse.description(),
                cardAfter.getCurrentBalance(),
                savedExpense.getId(),
                null,
                mappedResponse.createdAt()
        );
    }

    @Transactional
    public CardMovementResponse registerPayment(Long cardId, CardPaymentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CreditCard card = findOwnedCard(cardId, userId);

        int updatedRows = creditCardRepository.decrementBalance(cardId, request.amount());
        if (updatedRows == 0) {
            // Otro pago concurrente ya redujo el saldo entre la lectura de la tarjeta y este
            // UPDATE atómico; se rechaza sin persistir el CardMovement.
            throw new IllegalArgumentException(PAYMENT_OVER_BALANCE_MESSAGE);
        }

        LocalDate paymentDate = request.date() != null ? request.date() : LocalDate.now();

        CardMovement movement = cardMovementMapper.toEntity(request);
        movement.setCard(card);
        movement.setType(CardMovementType.PAYMENT);
        movement.setDate(paymentDate);
        CardMovement savedMovement = cardMovementRepository.save(movement);

        CreditCard cardAfter = findCardById(cardId);
        CardMovementResponse mappedResponse = cardMovementMapper.toResponse(savedMovement);
        return new CardMovementResponse(
                mappedResponse.id(),
                mappedResponse.cardId(),
                mappedResponse.type(),
                mappedResponse.amount(),
                mappedResponse.date(),
                mappedResponse.description(),
                cardAfter.getCurrentBalance(),
                null,
                null,
                mappedResponse.createdAt()
        );
    }

    private CreditCard findOwnedCard(Long cardId, Long userId) {
        return creditCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_MESSAGE));
    }

    private CreditCard findCardById(Long cardId) {
        return creditCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_MESSAGE));
    }
}
