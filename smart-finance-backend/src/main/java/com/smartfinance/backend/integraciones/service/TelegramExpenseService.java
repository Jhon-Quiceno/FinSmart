package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.common.security.InMemoryRateLimiter;
import com.smartfinance.backend.extractos.service.dedup.DescriptionSimilarity;
import com.smartfinance.backend.gastos.model.dto.ExpenseRequest;
import com.smartfinance.backend.gastos.model.dto.ExpenseResponse;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.gastos.service.ExpenseService;
import com.smartfinance.backend.ia.model.dto.MovementClassification;
import com.smartfinance.backend.ia.service.AiCategorizationService;
import com.smartfinance.backend.ingresos.model.dto.IncomeRequest;
import com.smartfinance.backend.ingresos.model.dto.IncomeResponse;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.ingresos.service.IncomeService;
import com.smartfinance.backend.integraciones.exception.TelegramChatNotLinkedException;
import com.smartfinance.backend.integraciones.exception.TelegramRateLimitExceededException;
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
import java.time.Duration;
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

    /** Protege contra abuso/spam y contra gastar la cuota de IA por un loop accidental. */
    private static final int MAX_MESSAGES_PER_MINUTE = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_MESSAGE =
            "Estás enviando mensajes muy rápido. Esperá un minuto e intentá de nuevo.";

    private final TelegramLinkRepository telegramLinkRepository;
    private final TelegramMessageParser messageParser;
    private final AiCategorizationService aiCategorizationService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter();

    public TelegramExpenseService(
            TelegramLinkRepository telegramLinkRepository,
            TelegramMessageParser messageParser,
            AiCategorizationService aiCategorizationService,
            ExpenseService expenseService,
            IncomeService incomeService,
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository
    ) {
        this.telegramLinkRepository = telegramLinkRepository;
        this.messageParser = messageParser;
        this.aiCategorizationService = aiCategorizationService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
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
        if (!rateLimiter.tryConsume(chatId, MAX_MESSAGES_PER_MINUTE, RATE_LIMIT_WINDOW)) {
            throw new TelegramRateLimitExceededException(RATE_LIMIT_MESSAGE);
        }

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
        LocalDate today = LocalDate.now();
        boolean possibleDuplicate = expenseRepository.findByUserAndPeriod(userId, today, today).stream()
                .anyMatch(existing -> isLikelyDuplicate(parsed, existing.getAmount(), existing.getDescription()));

        ExpenseResponse expense = expenseService.createExpense(userId, new ExpenseRequest(
                parsed.amount(),
                parsed.description(),
                today,
                PaymentMethodType.OTHER,
                classification.categoryId()
        ));

        return "✅ Gasto registrado: %s — $%s (%s)%s".formatted(
                expense.description(),
                formatAmount(expense.amount()),
                expense.categoryName() != null ? expense.categoryName() : "sin categoría",
                possibleDuplicate ? "\n\n⚠️ Parece similar a un gasto que ya registraste hoy — revisalo en la app si fue sin querer." : ""
        );
    }

    private String registerIncome(Long userId, TelegramMessageParser.ParsedMessage parsed, MovementClassification classification) {
        LocalDate today = LocalDate.now();
        boolean possibleDuplicate = incomeRepository.findByUserAndPeriod(userId, today, today).stream()
                .anyMatch(existing -> isLikelyDuplicate(parsed, existing.getAmount(), existing.getDescription()));

        IncomeResponse income = incomeService.createIncome(userId, new IncomeRequest(
                parsed.amount(),
                parsed.description(),
                today,
                classification.categoryId()
        ));

        return "✅ Ingreso registrado: %s — $%s (%s)%s".formatted(
                income.description(),
                formatAmount(income.amount()),
                income.categoryName() != null ? income.categoryName() : "sin categoría",
                possibleDuplicate ? "\n\n⚠️ Parece similar a un ingreso que ya registraste hoy — revisalo en la app si fue sin querer." : ""
        );
    }

    /**
     * Mismo criterio que {@code DuplicateDetector} (extractos), simplificado a "mismo día": el
     * bot registra en el momento, no importa un extracto con fechas históricas, así que la
     * ventana de tolerancia de fecha de ese detector (±3 días) no aplica acá — alcanza con
     * comparar contra lo ya registrado hoy mismo.
     */
    private static boolean isLikelyDuplicate(TelegramMessageParser.ParsedMessage parsed, BigDecimal existingAmount, String existingDescription) {
        return parsed.amount().compareTo(existingAmount) == 0
                && DescriptionSimilarity.isSimilar(parsed.description(), existingDescription);
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
