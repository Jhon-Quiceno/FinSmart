package com.smartfinance.backend.tarjetas.service;

import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.tarjetas.mapper.CardMovementMapper;
import com.smartfinance.backend.tarjetas.mapper.InstallmentMapper;
import com.smartfinance.backend.tarjetas.model.dto.CardMovementResponse;
import com.smartfinance.backend.tarjetas.model.dto.CardPaymentRequest;
import com.smartfinance.backend.tarjetas.model.dto.CardPurchaseRequest;
import com.smartfinance.backend.tarjetas.model.dto.InstallmentResponse;
import com.smartfinance.backend.tarjetas.model.entity.CardMovement;
import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.model.entity.Installment;
import com.smartfinance.backend.tarjetas.model.entity.InstallmentPlan;
import com.smartfinance.backend.tarjetas.repository.CardMovementRepository;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
import com.smartfinance.backend.tarjetas.repository.InstallmentPlanRepository;
import com.smartfinance.backend.tarjetas.repository.InstallmentRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
 * más cuotas ({@code installmentCount >= 2}) delegan el cronograma a {@link AmortizationService}
 * y persisten el {@link InstallmentPlan} resultante con la tasa de la tarjeta congelada al
 * momento de la compra (spec "Tasa congelada ante cambios posteriores").
 *
 * <p>{@link #registerPayment} es el mirror simétrico para pagos: decrementa el saldo vía
 * {@link CreditCardRepository#decrementBalance} y NO crea un {@link Expense} — un pago de
 * tarjeta no es un gasto nuevo, ya se contabilizó como {@code PURCHASE}/{@code INSTALLMENT_PURCHASE}
 * en su momento.
 */
@Service
public class CardMovementService {

    private static final String PURCHASE_OVER_LIMIT_MESSAGE =
            "La compra supera el cupo disponible de la tarjeta";
    private static final String PAYMENT_OVER_BALANCE_MESSAGE =
            "El pago no puede superar el saldo actual de la tarjeta";
    private static final String CARD_NOT_FOUND_MESSAGE = "Tarjeta no encontrada";
    private static final String INSTALLMENT_PLAN_NOT_FOUND_MESSAGE = "Plan de cuotas no encontrado";

    private final CardMovementRepository cardMovementRepository;
    private final CreditCardRepository creditCardRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CardMovementMapper cardMovementMapper;
    private final AmortizationService amortizationService;
    private final InstallmentPlanRepository installmentPlanRepository;
    private final InstallmentRepository installmentRepository;
    private final InstallmentMapper installmentMapper;

    public CardMovementService(
            CardMovementRepository cardMovementRepository,
            CreditCardRepository creditCardRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            CardMovementMapper cardMovementMapper,
            AmortizationService amortizationService,
            InstallmentPlanRepository installmentPlanRepository,
            InstallmentRepository installmentRepository,
            InstallmentMapper installmentMapper
    ) {
        this.cardMovementRepository = cardMovementRepository;
        this.creditCardRepository = creditCardRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.cardMovementMapper = cardMovementMapper;
        this.amortizationService = amortizationService;
        this.installmentPlanRepository = installmentPlanRepository;
        this.installmentRepository = installmentRepository;
        this.installmentMapper = installmentMapper;
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

        boolean isInstallmentPurchase = request.installmentCount() != null && request.installmentCount() >= 2;
        LocalDate purchaseDate = request.date() != null ? request.date() : LocalDate.now();

        // El cronograma se calcula ANTES de tocar el saldo o persistir nada: si el monto es
        // demasiado bajo para la cantidad de cuotas elegida (resguardo anti-capital-negativo de
        // AmortizationService), la compra se rechaza con cero efectos secundarios, igual que el
        // rechazo por cupo insuficiente más abajo.
        List<Installment> schedule = isInstallmentPurchase
                ? amortizationService.buildSchedule(request.amount(), request.installmentCount(), card.getMonthlyRate(), purchaseDate, card)
                : null;

        int updatedRows = creditCardRepository.incrementBalanceWithinLimit(cardId, request.amount());
        if (updatedRows == 0) {
            // Otra compra concurrente consumió el cupo disponible entre la lectura de la
            // tarjeta y este UPDATE atómico; se rechaza sin persistir el CardMovement.
            throw new IllegalArgumentException(PURCHASE_OVER_LIMIT_MESSAGE);
        }

        CardMovement movement = cardMovementMapper.toEntity(request);
        movement.setCard(card);
        movement.setType(isInstallmentPurchase ? CardMovementType.INSTALLMENT_PURCHASE : CardMovementType.PURCHASE);
        movement.setDate(purchaseDate);
        CardMovement savedMovement = cardMovementRepository.save(movement);

        Long installmentPlanId = null;
        if (isInstallmentPurchase) {
            InstallmentPlan plan = new InstallmentPlan();
            plan.setMovement(savedMovement);
            plan.setInstallmentCount(request.installmentCount());
            // Tasa vigente de la tarjeta AHORA MISMO, congelada — spec "Tasa congelada ante
            // cambios posteriores": si CreditCard.monthlyRate cambia después, este plan conserva
            // el valor de hoy sin recalcular.
            plan.setRateAtPurchase(card.getMonthlyRate());
            schedule.forEach(installment -> installment.setPlan(plan));
            plan.setInstallments(schedule);
            InstallmentPlan savedPlan = installmentPlanRepository.save(plan);
            installmentPlanId = savedPlan.getId();
        }

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
                installmentPlanId,
                mappedResponse.createdAt()
        );
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> getInstallments(Long cardId, Long movementId) {
        Long userId = SecurityUtils.getCurrentUserId();
        findOwnedCard(cardId, userId);

        InstallmentPlan plan = installmentPlanRepository.findByMovement_Id(movementId)
                .filter(candidate -> candidate.getMovement().getCard().getId().equals(cardId))
                .orElseThrow(() -> new ResourceNotFoundException(INSTALLMENT_PLAN_NOT_FOUND_MESSAGE));

        return installmentRepository.findAllByPlan_IdOrderByNumber(plan.getId())
                .stream()
                .map(installmentMapper::toResponse)
                .toList();
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
