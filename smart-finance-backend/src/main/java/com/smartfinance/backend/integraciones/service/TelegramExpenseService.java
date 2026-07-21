package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.common.security.InMemoryRateLimiter;
import com.smartfinance.backend.extractos.service.dedup.DescriptionSimilarity;
import com.smartfinance.backend.gastos.model.dto.ExpenseRequest;
import com.smartfinance.backend.gastos.model.dto.ExpenseResponse;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.CategoryTotalProjection;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.gastos.service.ExpenseService;
import com.smartfinance.backend.ia.model.dto.MovementClassification;
import com.smartfinance.backend.ia.model.dto.ReceiptExtraction;
import com.smartfinance.backend.ia.model.dto.SummaryPeriod;
import com.smartfinance.backend.ia.model.dto.SummaryQueryIntent;
import com.smartfinance.backend.ia.service.AiCategorizationService;
import com.smartfinance.backend.ia.service.FinancialSummaryQueryService;
import com.smartfinance.backend.ia.service.ReceiptExtractionService;
import com.smartfinance.backend.ingresos.model.dto.IncomeRequest;
import com.smartfinance.backend.ingresos.model.dto.IncomeResponse;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.ingresos.service.IncomeService;
import com.smartfinance.backend.integraciones.exception.TelegramChatNotLinkedException;
import com.smartfinance.backend.integraciones.exception.TelegramRateLimitExceededException;
import com.smartfinance.backend.integraciones.model.entity.TelegramLink;
import com.smartfinance.backend.integraciones.repository.TelegramLinkRepository;
import com.smartfinance.backend.usuario.model.entity.User;
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
import java.util.Optional;

/**
 * Registra un ingreso o un gasto a partir de un mensaje de texto libre, o de la foto de un recibo,
 * recibidos desde el bot de Telegram (orquestado por n8n).
 *
 * <p>El {@code chatId} debe estar previamente vinculado a un usuario (ver
 * {@code TelegramLinkService#confirmLink}); de lo contrario se rechaza con
 * {@link TelegramChatNotLinkedException}. Para el flujo de texto ({@link #registerFromMessage}),
 * una sola llamada a IA ({@link AiCategorizationService#classifyMovement}) decide si el mensaje es
 * un ingreso o un gasto, y sugiere la categoría. Para el flujo de foto ({@link #registerFromPhoto}),
 * una sola llamada a un modelo con capacidad de visión ({@link ReceiptExtractionService#extractFromImage})
 * extrae comercio, monto, tipo de movimiento y categoría directamente de la imagen. En ambos casos
 * el resultado determina cuál de los dos servicios de dominio ({@link ExpenseService} o
 * {@link IncomeService}) registra el movimiento. El método de pago del gasto siempre se registra
 * como {@link PaymentMethodType#OTHER} y la fecha como la fecha actual, ya que ni el texto ni la
 * foto de Telegram permiten especificar ninguna de las dos.
 *
 * <p>{@link #registerFromMessage} también responde preguntas en lenguaje natural sobre las
 * finanzas del usuario (ej. {@code "¿Cuánto gasté en comida este mes?"}, {@code "resumen"}): antes
 * de intentar interpretar el texto como un movimiento a registrar, {@code TelegramIntentDetector}
 * decide con un heurístico barato (sin IA) si el mensaje parece una consulta; de ser así, se
 * resuelve con {@link FinancialSummaryQueryService} y una consulta de solo lectura, en lugar de
 * registrar (o rechazar) un movimiento inexistente.
 *
 * <p>Ambos flujos de registro comparten la misma instancia de {@link InMemoryRateLimiter}, con la
 * misma clave ({@code chatId}): un usuario no puede eludir el límite de mensajes por minuto
 * alternando entre mensajes de texto, fotos, y consultas de resumen desde el mismo chat (las tres
 * rutas de {@link #registerFromMessage} y {@link #registerFromPhoto} consumen del mismo limitador).
 */
@Service
public class TelegramExpenseService {

    private static final Logger log = LoggerFactory.getLogger(TelegramExpenseService.class);

    /** Protege contra abuso/spam y contra gastar la cuota de IA por un loop accidental. */
    private static final int MAX_MESSAGES_PER_MINUTE = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_MESSAGE =
            "Estás enviando mensajes muy rápido. Esperá un minuto e intentá de nuevo.";

    private static final String NOT_A_RECEIPT_MESSAGE =
            "No pude leer un recibo en esa imagen. Si es un comprobante válido, probá con más luz o "
                    + "escribí el gasto a mano (ej. \"Uber 15000\").";

    // Mismos límites, y mismo criterio de "filtro barato sin llamada a IA adicional" que
    // TelegramMessageParser#MIN_PLAUSIBLE_AMOUNT/MAX_PLAUSIBLE_AMOUNT — duplicados acá en lugar de
    // extraídos a un tipo compartido porque el origen del monto es distinto (un modelo de visión,
    // no un regex sobre texto propio), aunque el rango razonable para una transacción personal es
    // el mismo.
    private static final BigDecimal MIN_PLAUSIBLE_AMOUNT = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_PLAUSIBLE_AMOUNT = BigDecimal.valueOf(500_000_000);
    private static final String PHOTO_AMOUNT_TOO_LOW_MESSAGE =
            "El monto que leí en la imagen parece muy bajo. Verificá la foto o escribí el gasto a mano.";
    private static final String PHOTO_AMOUNT_TOO_HIGH_MESSAGE =
            "El monto que leí en la imagen parece demasiado alto. Si es correcto, registralo desde la app.";

    private final TelegramLinkRepository telegramLinkRepository;
    private final TelegramMessageParser messageParser;
    private final AiCategorizationService aiCategorizationService;
    private final ReceiptExtractionService receiptExtractionService;
    private final FinancialSummaryQueryService financialSummaryQueryService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter();

    public TelegramExpenseService(
            TelegramLinkRepository telegramLinkRepository,
            TelegramMessageParser messageParser,
            AiCategorizationService aiCategorizationService,
            ReceiptExtractionService receiptExtractionService,
            FinancialSummaryQueryService financialSummaryQueryService,
            ExpenseService expenseService,
            IncomeService incomeService,
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository
    ) {
        this.telegramLinkRepository = telegramLinkRepository;
        this.messageParser = messageParser;
        this.aiCategorizationService = aiCategorizationService;
        this.receiptExtractionService = receiptExtractionService;
        this.financialSummaryQueryService = financialSummaryQueryService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
    }

    /**
     * @param chatId identificador del chat de Telegram que envió el mensaje
     * @param text   texto libre del mensaje: un movimiento a registrar (ej. {@code "Uber 15000"} o
     *               {@code "Me pagaron 50000"}), o una pregunta sobre las finanzas del usuario (ej.
     *               {@code "¿Cuánto gasté en comida?"} o {@code "resumen"}) — ver
     *               {@link TelegramIntentDetector#looksLikeSummaryQuery}
     * @return el mensaje de confirmación (registro) o la respuesta calculada (consulta) en español,
     *         listo para reenviar al usuario por Telegram
     * @throws TelegramChatNotLinkedException si {@code chatId} no está vinculado a ningún usuario
     * @throws com.smartfinance.backend.integraciones.exception.TelegramMessageParseException si
     *         {@code text} no parece una consulta y tampoco contiene un monto interpretable
     * @throws com.smartfinance.backend.integraciones.exception.TelegramImplausibleMovementException
     *         si {@code text} no parece una consulta, tiene un monto interpretable, pero resulta
     *         implausible como movimiento real (ver {@code TelegramMessageParser#parse})
     */
    @Transactional
    public String registerFromMessage(String chatId, String text) {
        if (!rateLimiter.tryConsume(chatId, MAX_MESSAGES_PER_MINUTE, RATE_LIMIT_WINDOW)) {
            throw new TelegramRateLimitExceededException(RATE_LIMIT_MESSAGE);
        }

        Long userId = resolveUserId(chatId);
        if (TelegramIntentDetector.looksLikeSummaryQuery(text)) {
            return buildSummaryReply(userId, text);
        }

        TelegramMessageParser.ParsedMessage parsed = messageParser.parse(text);
        MovementClassification classification = classifySafely(userId, parsed);

        return classification.type() == CategoryType.INCOME
                ? registerIncomeAndBuildReply(userId, parsed.amount(), parsed.description(), classification.categoryId(), "✅ Ingreso registrado")
                : registerExpenseAndBuildReply(userId, parsed.amount(), parsed.description(), classification.categoryId(), "✅ Gasto registrado");
    }

    /**
     * Resuelve y formatea la respuesta a una pregunta en lenguaje natural sobre las finanzas del
     * usuario (ej. {@code "¿Cuánto gasté en comida este mes?"}, {@code "resumen"}). No crea ni
     * modifica ningún {@code Expense}/{@code Income}: es un cálculo de solo lectura dentro de la
     * transacción ya abierta por {@link #registerFromMessage}.
     */
    private String buildSummaryReply(Long userId, String text) {
        SummaryQueryIntent intent = financialSummaryQueryService.parseQuery(userId, text);
        LocalDate today = LocalDate.now();
        LocalDate start = switch (intent.period()) {
            case TODAY -> today;
            case WEEK -> today.minusDays(6);
            case MONTH -> today.withDayOfMonth(1);
        };

        if (intent.movementType() == CategoryType.EXPENSE) {
            return buildExpenseSummaryReply(userId, intent.period(), start, today, intent.categoryName());
        }
        if (intent.movementType() == CategoryType.INCOME) {
            return buildIncomeSummaryReply(userId, intent.period(), start, today, intent.categoryName());
        }
        return buildBalanceSummaryReply(userId, intent.period(), start, today);
    }

    private String buildExpenseSummaryReply(Long userId, SummaryPeriod period, LocalDate start, LocalDate end, String categoryName) {
        String periodLabel = periodLabel(period);
        if (categoryName == null) {
            BigDecimal total = expenseRepository.sumAmountByUserAndPeriod(userId, start, end);
            return "📊 Gastaste $%s en total %s.".formatted(formatAmount(total), periodLabel);
        }

        Optional<CategoryTotalProjection> matched = expenseRepository.findTopCategoriesByUserAndPeriod(userId, start, end)
                .stream()
                .filter(row -> categoryName.equalsIgnoreCase(row.getCategoryName()))
                .findFirst();
        if (matched.isEmpty()) {
            return "📊 No encontré gastos en \"%s\" %s.".formatted(categoryName, periodLabel);
        }
        return "📊 Gastaste $%s en %s %s.".formatted(formatAmount(matched.get().getTotal()), matched.get().getCategoryName(), periodLabel);
    }

    /**
     * {@link IncomeRepository} no tiene un equivalente a
     * {@link ExpenseRepository#findTopCategoriesByUserAndPeriod}: ante una categoría mencionada, se
     * degrada al total general en lugar de inventar una cifra por categoría que no se puede
     * calcular.
     */
    private String buildIncomeSummaryReply(Long userId, SummaryPeriod period, LocalDate start, LocalDate end, String categoryName) {
        String periodLabel = periodLabel(period);
        BigDecimal total = incomeRepository.sumAmountByUserAndPeriod(userId, start, end);
        return categoryName == null
                ? "📊 Ingresaste $%s %s.".formatted(formatAmount(total), periodLabel)
                : "📊 Ingresaste $%s en total %s (todavía no tengo el desglose de ingresos por categoría)."
                        .formatted(formatAmount(total), periodLabel);
    }

    private String buildBalanceSummaryReply(Long userId, SummaryPeriod period, LocalDate start, LocalDate end) {
        String periodLabel = periodLabel(period);
        BigDecimal income = incomeRepository.sumAmountByUserAndPeriod(userId, start, end);
        BigDecimal expense = expenseRepository.sumAmountByUserAndPeriod(userId, start, end);
        BigDecimal net = income.subtract(expense);
        String sign = net.signum() < 0 ? "-" : "+";

        return "📊 Balance de %s: ingresos $%s, gastos $%s (neto: %s$%s).".formatted(
                periodLabel, formatAmount(income), formatAmount(expense), sign, formatAmount(net.abs())
        );
    }

    private static String periodLabel(SummaryPeriod period) {
        return switch (period) {
            case TODAY -> "hoy";
            case WEEK -> "esta semana";
            case MONTH -> "este mes";
        };
    }

    /**
     * @param chatId   identificador del chat de Telegram que envió la foto
     * @param imageUrl la imagen del recibo, como URL {@code https://} o data URI
     *                 {@code data:image/...;base64,...} (ver {@link ReceiptExtractionService})
     * @return el mensaje de confirmación en español, listo para reenviar al usuario por Telegram
     * @throws TelegramChatNotLinkedException si {@code chatId} no está vinculado a ningún usuario
     * @throws com.smartfinance.backend.integraciones.exception.TelegramRateLimitExceededException
     *         si {@code chatId} superó el límite de mensajes por minuto (compartido con
     *         {@link #registerFromMessage})
     */
    @Transactional
    public String registerFromPhoto(String chatId, String imageUrl) {
        if (!rateLimiter.tryConsume(chatId, MAX_MESSAGES_PER_MINUTE, RATE_LIMIT_WINDOW)) {
            throw new TelegramRateLimitExceededException(RATE_LIMIT_MESSAGE);
        }

        Long userId = resolveUserId(chatId);
        ReceiptExtraction extraction = extractSafely(userId, imageUrl);

        if (!extraction.isReceipt()) {
            return NOT_A_RECEIPT_MESSAGE;
        }
        if (extraction.amount().compareTo(MIN_PLAUSIBLE_AMOUNT) < 0) {
            return PHOTO_AMOUNT_TOO_LOW_MESSAGE;
        }
        if (extraction.amount().compareTo(MAX_PLAUSIBLE_AMOUNT) > 0) {
            return PHOTO_AMOUNT_TOO_HIGH_MESSAGE;
        }

        return extraction.movementType() == CategoryType.INCOME
                ? registerIncomeAndBuildReply(userId, extraction.amount(), extraction.description(), extraction.categoryId(), "✅ Ingreso registrado desde la foto")
                : registerExpenseAndBuildReply(userId, extraction.amount(), extraction.description(), extraction.categoryId(), "✅ Gasto registrado desde la foto");
    }

    private Long resolveUserId(String chatId) {
        return telegramLinkRepository.findByTelegramChatId(chatId)
                .map(TelegramLink::getUser)
                .map(User::getId)
                .orElseThrow(() -> new TelegramChatNotLinkedException(
                        "Todavía no vinculaste tu cuenta. Generá un código desde la app, en "
                                + "Configuración, y enviámelo para vincular tu chat."));
    }

    private String registerExpenseAndBuildReply(Long userId, BigDecimal amount, String description, Long categoryId, String successLabel) {
        LocalDate today = LocalDate.now();
        boolean possibleDuplicate = expenseRepository.findByUserAndPeriod(userId, today, today).stream()
                .anyMatch(existing -> isLikelyDuplicate(amount, description, existing.getAmount(), existing.getDescription()));

        ExpenseResponse expense = expenseService.createExpense(userId, new ExpenseRequest(
                amount,
                description,
                today,
                PaymentMethodType.OTHER,
                categoryId
        ));

        return "%s: %s — $%s (%s)%s".formatted(
                successLabel,
                expense.description(),
                formatAmount(expense.amount()),
                expense.categoryName() != null ? expense.categoryName() : "sin categoría",
                possibleDuplicate ? "\n\n⚠️ Parece similar a un gasto que ya registraste hoy — revisalo en la app si fue sin querer." : ""
        );
    }

    private String registerIncomeAndBuildReply(Long userId, BigDecimal amount, String description, Long categoryId, String successLabel) {
        LocalDate today = LocalDate.now();
        boolean possibleDuplicate = incomeRepository.findByUserAndPeriod(userId, today, today).stream()
                .anyMatch(existing -> isLikelyDuplicate(amount, description, existing.getAmount(), existing.getDescription()));

        IncomeResponse income = incomeService.createIncome(userId, new IncomeRequest(
                amount,
                description,
                today,
                categoryId
        ));

        return "%s: %s — $%s (%s)%s".formatted(
                successLabel,
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
    private static boolean isLikelyDuplicate(BigDecimal amount, String description, BigDecimal existingAmount, String existingDescription) {
        return amount.compareTo(existingAmount) == 0
                && DescriptionSimilarity.isSimilar(description, existingDescription);
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

    /**
     * Una foto de recibo no debe fallar con un error técnico solo porque el proveedor de IA de
     * visión esté caído, mal configurado o rechace la imagen: cualquier falla se degrada al mismo
     * resultado "no es un recibo" que ya usa {@link ReceiptExtractionService} para una imagen
     * ilegible, en lugar de propagar la excepción.
     *
     * <p>A diferencia de {@link #classifySafely} (que igual registra el gasto, sin categoría, ante
     * una falla de IA), acá no hay ningún monto que registrar cuando la extracción falla: no tiene
     * sentido inventar un movimiento sin datos reales, así que el resultado es simplemente pedirle
     * al usuario que lo escriba a mano.
     */
    private ReceiptExtraction extractSafely(Long userId, String imageUrl) {
        try {
            return receiptExtractionService.extractFromImage(userId, imageUrl);
        } catch (RuntimeException ex) {
            log.warn("telegram_receipt_extraction_failed userId={}", userId, ex);
            return ReceiptExtraction.notAReceipt();
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
