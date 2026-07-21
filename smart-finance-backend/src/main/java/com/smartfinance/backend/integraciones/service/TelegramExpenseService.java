package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.gastos.model.dto.ExpenseRequest;
import com.smartfinance.backend.gastos.model.dto.ExpenseResponse;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.service.ExpenseService;
import com.smartfinance.backend.ia.model.dto.MovementClassification;
import com.smartfinance.backend.ia.service.AiCategorizationService;
import com.smartfinance.backend.ingresos.model.dto.IncomeRequest;
import com.smartfinance.backend.ingresos.model.dto.IncomeResponse;
import com.smartfinance.backend.ingresos.service.IncomeService;
import com.smartfinance.backend.integraciones.exception.TelegramChatNotLinkedException;
import com.smartfinance.backend.integraciones.model.entity.TelegramLink;
import com.smartfinance.backend.integraciones.repository.TelegramLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Registra un ingreso o un gasto a partir de un mensaje de texto libre recibido desde el bot de
 * Telegram (orquestado por n8n).
 *
 * <p>El {@code chatId} debe estar previamente vinculado a un usuario (ver
 * {@code TelegramLinkService#confirmLink}); de lo contrario se rechaza con
 * {@link TelegramChatNotLinkedException}. Una sola llamada a IA ({@link AiCategorizationService#classifyMovement})
 * decide si el mensaje es un ingreso o un gasto, y sugiere la categoría; el resultado determina
 * cuál de los dos servicios de dominio ({@link ExpenseService} o {@link IncomeService}) registra
 * el movimiento. El método de pago del gasto siempre se registra como {@link PaymentMethodType#OTHER}
 * y la fecha como la fecha actual, ya que el mensaje de Telegram no permite especificar ninguna de
 * las dos.
 */
@Service
public class TelegramExpenseService {

    private static final Logger log = LoggerFactory.getLogger(TelegramExpenseService.class);

    private final TelegramLinkRepository telegramLinkRepository;
    private final TelegramMessageParser messageParser;
    private final AiCategorizationService aiCategorizationService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;

    public TelegramExpenseService(
            TelegramLinkRepository telegramLinkRepository,
            TelegramMessageParser messageParser,
            AiCategorizationService aiCategorizationService,
            ExpenseService expenseService,
            IncomeService incomeService
    ) {
        this.telegramLinkRepository = telegramLinkRepository;
        this.messageParser = messageParser;
        this.aiCategorizationService = aiCategorizationService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
    }

    /**
     * @param chatId identificador del chat de Telegram que envió el mensaje
     * @param text   texto libre del mensaje (por ejemplo, {@code "Uber 15000"} o {@code "Me pagaron 50000"})
     * @return el mensaje de confirmación en español, listo para reenviar al usuario por Telegram
     * @throws TelegramChatNotLinkedException si {@code chatId} no está vinculado a ningún usuario
     * @throws com.smartfinance.backend.integraciones.exception.TelegramMessageParseException si
     *         {@code text} no contiene un monto interpretable
     * @throws com.smartfinance.backend.integraciones.exception.TelegramImplausibleMovementException
     *         si {@code text} tiene un monto interpretable pero resulta implausible como movimiento
     *         real (ver {@code TelegramMessageParser#parse})
     */
    @Transactional
    public String registerFromMessage(String chatId, String text) {
        Long userId = telegramLinkRepository.findByTelegramChatId(chatId)
                .map(TelegramLink::getUser)
                .map(user -> user.getId())
                .orElseThrow(() -> new TelegramChatNotLinkedException(
                        "Todavía no vinculaste tu cuenta. Generá un código desde la app, en "
                                + "Configuración, y enviámelo para vincular tu chat."));

        TelegramMessageParser.ParsedMessage parsed = messageParser.parse(text);
        MovementClassification classification = classifySafely(userId, parsed);

        return classification.type() == CategoryType.INCOME
                ? registerIncome(userId, parsed, classification)
                : registerExpense(userId, parsed, classification);
    }

    private String registerExpense(Long userId, TelegramMessageParser.ParsedMessage parsed, MovementClassification classification) {
        ExpenseResponse expense = expenseService.createExpense(userId, new ExpenseRequest(
                parsed.amount(),
                parsed.description(),
                LocalDate.now(),
                PaymentMethodType.OTHER,
                classification.categoryId()
        ));

        return "✅ Gasto registrado: %s — $%s (%s)".formatted(
                expense.description(),
                formatAmount(expense.amount()),
                expense.categoryName() != null ? expense.categoryName() : "sin categoría"
        );
    }

    private String registerIncome(Long userId, TelegramMessageParser.ParsedMessage parsed, MovementClassification classification) {
        IncomeResponse income = incomeService.createIncome(userId, new IncomeRequest(
                parsed.amount(),
                parsed.description(),
                LocalDate.now(),
                classification.categoryId()
        ));

        return "✅ Ingreso registrado: %s — $%s (%s)".formatted(
                income.description(),
                formatAmount(income.amount()),
                income.categoryName() != null ? income.categoryName() : "sin categoría"
        );
    }

    /**
     * Un movimiento registrado por Telegram no debe fallar porque el proveedor de IA esté caído o
     * mal configurado: se intenta clasificar automáticamente (tipo + categoría), pero cualquier
     * falla se degrada a "gasto sin categoría" en lugar de propagar el error y perder el registro
     * del movimiento.
     *
     * <p>{@link AiCategorizationService#classifyMovement} ya degrada de forma interna ante una
     * respuesta del modelo mal formada; este {@code try/catch} es una defensa adicional para
     * cualquier falla que esa capa no atrape (por ejemplo, {@link AiCategorizationService#classifyMovement}
     * propaga hacia arriba las fallas del proveedor de IA, ver su Javadoc).
     */
    private MovementClassification classifySafely(Long userId, TelegramMessageParser.ParsedMessage parsed) {
        try {
            return aiCategorizationService.classifyMovement(userId, parsed.description(), parsed.amount());
        } catch (RuntimeException ex) {
            log.warn("telegram_classify_movement_failed userId={}", userId, ex);
            return new MovementClassification(CategoryType.EXPENSE, null, null);
        }
    }

    /** Formatea el monto como un valor entero con separador de miles {@code .} (ej. {@code "15.000"}). */
    private static String formatAmount(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(rounded);
    }
}
