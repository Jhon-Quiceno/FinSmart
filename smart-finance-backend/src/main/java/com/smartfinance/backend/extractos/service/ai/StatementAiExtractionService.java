package com.smartfinance.backend.extractos.service.ai;

import com.smartfinance.backend.extractos.exception.StatementExtractionException;
import com.smartfinance.backend.extractos.model.MovementType;
import com.smartfinance.backend.extractos.model.ParsedTransaction;
import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.service.AiUsageEventService;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import com.smartfinance.backend.ia.service.ai.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pide al proveedor de IA configurado que extraiga los movimientos (ingresos y gastos) de un
 * extracto bancario, a partir de su texto plano ya extraído por
 * {@code StatementTextExtractionService}.
 *
 * <p>Sigue el mismo patrón que {@code AiCategorizationService}: construye un prompt de sistema
 * que restringe estrictamente el formato de respuesta, llama a {@link AiChatOrchestrator#complete}
 * y parsea la respuesta de forma defensiva, ya que un modelo de lenguaje nunca garantiza al 100%
 * cumplir el formato instruido.
 */
@Service
public class StatementAiExtractionService {

    private static final Logger log = LoggerFactory.getLogger(StatementAiExtractionService.class);

    /**
     * Límite de caracteres del texto del extracto enviado al modelo: cuida el presupuesto de
     * tokens del prompt (evita un costo desproporcionado o un prompt demasiado grande para el
     * modelo) a cambio de, en extractos muy extensos, dejar de extraer los movimientos que caen
     * después del corte.
     */
    private static final int MAX_STATEMENT_TEXT_LENGTH = 20_000;

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
    private static final TypeReference<List<RawRow>> RAW_ROW_LIST_TYPE = new TypeReference<>() {
    };

    private final AiChatOrchestrator aiChatOrchestrator;
    private final AiUsageEventService aiUsageEventService;
    private final ObjectMapper objectMapper;

    public StatementAiExtractionService(
            AiChatOrchestrator aiChatOrchestrator,
            AiUsageEventService aiUsageEventService,
            ObjectMapper objectMapper
    ) {
        this.aiChatOrchestrator = aiChatOrchestrator;
        this.aiUsageEventService = aiUsageEventService;
        this.objectMapper = objectMapper;
    }

    /**
     * @param statementText     texto plano del extracto (truncado internamente si excede
     *                          {@link #MAX_STATEMENT_TEXT_LENGTH})
     * @param incomeCategories  categorías de tipo {@link CategoryType#INCOME} del usuario
     * @param expenseCategories categorías de tipo {@link CategoryType#EXPENSE} del usuario
     * @param userId            id del usuario, para registrar el evento de uso de IA
     * @return los movimientos extraídos y normalizados; las filas inválidas se descartan (ver
     *         {@link #normalizeRow})
     * @throws StatementExtractionException si la respuesta del modelo no se pudo interpretar como
     *                                       un arreglo JSON de movimientos
     */
    public List<ParsedTransaction> extract(
            String statementText,
            List<Category> incomeCategories,
            List<Category> expenseCategories,
            Long userId
    ) {
        List<ChatMessage> messages = List.of(
                ChatMessage.system(buildSystemPrompt(incomeCategories, expenseCategories)),
                ChatMessage.user(truncate(statementText))
        );

        ChatCompletionResult result = aiChatOrchestrator.complete(messages);
        aiUsageEventService.record(userId, result.providerName(), AiUsageEventType.STATEMENT_EXTRACT, totalTokens(result), null);

        List<RawRow> rawRows = parseRawRows(result.content());

        List<ParsedTransaction> transactions = new ArrayList<>();
        for (RawRow rawRow : rawRows) {
            normalizeRow(rawRow, incomeCategories, expenseCategories).ifPresent(transactions::add);
        }
        return transactions;
    }

    private static String buildSystemPrompt(List<Category> incomeCategories, List<Category> expenseCategories) {
        String incomeNames = joinNames(incomeCategories);
        String expenseNames = joinNames(expenseCategories);
        return "Eres un extractor de movimientos de extractos bancarios personales. A partir del texto de un "
                + "extracto bancario, identifica cada movimiento (ingreso o gasto) individual y devuelve "
                + "EXCLUSIVAMENTE un arreglo JSON válido, sin texto adicional, sin explicaciones ni bloques de "
                + "código markdown. Cada elemento del arreglo debe tener exactamente esta forma: "
                + "{\"date\":\"YYYY-MM-DD\",\"description\":\"texto\",\"amount\":numero positivo,"
                + "\"movementType\":\"INCOME\"|\"EXPENSE\",\"suggestedCategoryName\":\"texto\"|null}. "
                + "INCOME es dinero que entra a la cuenta (depósitos, transferencias recibidas, salario, etc.); "
                + "EXPENSE es dinero que sale (compras, pagos, retiros, etc.). El campo suggestedCategoryName debe "
                + "ser EXCLUSIVAMENTE uno de los siguientes nombres según el tipo de movimiento, o null si ninguno "
                + "corresponde. Categorías de ingreso disponibles: " + incomeNames + ". Categorías de gasto "
                + "disponibles: " + expenseNames + ". Ignora saldos, encabezados, totales y líneas de resumen: "
                + "extrae únicamente movimientos individuales.";
    }

    private static String joinNames(List<Category> categories) {
        if (categories.isEmpty()) {
            return "ninguna";
        }
        return categories.stream().map(Category::getName).collect(Collectors.joining(", "));
    }

    private static String truncate(String statementText) {
        if (statementText.length() <= MAX_STATEMENT_TEXT_LENGTH) {
            return statementText;
        }
        return statementText.substring(0, MAX_STATEMENT_TEXT_LENGTH);
    }

    /** Sums {@code promptTokens + completionTokens}, treating either as {@code 0} when the provider did not report it. */
    private static int totalTokens(ChatCompletionResult result) {
        int prompt = result.promptTokens() != null ? result.promptTokens() : 0;
        int completion = result.completionTokens() != null ? result.completionTokens() : 0;
        return prompt + completion;
    }

    private List<RawRow> parseRawRows(String rawResponse) {
        String jsonArray = extractJsonArray(rawResponse);
        try {
            return objectMapper.readValue(jsonArray, RAW_ROW_LIST_TYPE);
        } catch (JacksonException | IllegalArgumentException ex) {
            throw new StatementExtractionException(
                    "No se pudieron leer los movimientos del extracto. Intente de nuevo.", ex
            );
        }
    }

    /**
     * Limpia la respuesta cruda del modelo: recorta espacios, quita un posible bloque de código
     * markdown ({@code ```json ... ```} o {@code ``` ... ```}) y se queda con la subcadena entre
     * el primer {@code [} y el último {@code ]}, tolerando texto adicional antes o después del
     * arreglo JSON aunque el prompt lo prohíba explícitamente.
     */
    private static String extractJsonArray(String rawResponse) {
        if (rawResponse == null) {
            throw new StatementExtractionException("No se pudieron leer los movimientos del extracto. Intente de nuevo.");
        }
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("json")) {
                trimmed = trimmed.substring(4);
            }
            int closingFence = trimmed.lastIndexOf("```");
            if (closingFence >= 0) {
                trimmed = trimmed.substring(0, closingFence);
            }
            trimmed = trimmed.trim();
        }

        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start < 0 || end < start) {
            throw new StatementExtractionException("No se pudieron leer los movimientos del extracto. Intente de nuevo.");
        }
        return trimmed.substring(start, end + 1);
    }

    private Optional<ParsedTransaction> normalizeRow(
            RawRow rawRow,
            List<Category> incomeCategories,
            List<Category> expenseCategories
    ) {
        Optional<LocalDate> date = parseDate(rawRow.date());
        if (date.isEmpty()) {
            log.debug("statement_row_dropped reason=fecha_invalida raw={}", rawRow.date());
            return Optional.empty();
        }

        Optional<BigDecimal> amount = normalizeAmount(rawRow.amount());
        if (amount.isEmpty()) {
            log.debug("statement_row_dropped reason=monto_invalido raw={}", rawRow.amount());
            return Optional.empty();
        }

        Optional<MovementType> movementType = parseMovementType(rawRow.movementType());
        if (movementType.isEmpty()) {
            log.debug("statement_row_dropped reason=tipo_movimiento_invalido raw={}", rawRow.movementType());
            return Optional.empty();
        }

        List<Category> candidateCategories = movementType.get() == MovementType.INCOME ? incomeCategories : expenseCategories;
        Category matchedCategory = resolveCategory(rawRow.suggestedCategoryName(), candidateCategories);

        return Optional.of(new ParsedTransaction(
                date.get(),
                rawRow.description(),
                amount.get(),
                movementType.get(),
                matchedCategory != null ? matchedCategory.getId() : null,
                matchedCategory != null ? matchedCategory.getName() : null
        ));
    }

    /** Intenta {@code yyyy-MM-dd} primero, y como respaldo {@code dd/MM/yyyy}, ambos formatos frecuentes en extractos. */
    private static Optional<LocalDate> parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(rawDate.trim(), ISO_DATE));
        } catch (DateTimeParseException isoFailure) {
            try {
                return Optional.of(LocalDate.parse(rawDate.trim(), SLASH_DATE));
            } catch (DateTimeParseException slashFailure) {
                return Optional.empty();
            }
        }
    }

    private static Optional<BigDecimal> normalizeAmount(BigDecimal rawAmount) {
        if (rawAmount == null || rawAmount.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(rawAmount.abs());
    }

    private static Optional<MovementType> parseMovementType(String rawMovementType) {
        if (rawMovementType == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(MovementType.valueOf(rawMovementType.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static Category resolveCategory(String suggestedName, List<Category> candidates) {
        if (suggestedName == null || suggestedName.isBlank()) {
            return null;
        }
        return candidates.stream()
                .filter(category -> category.getName().equalsIgnoreCase(suggestedName.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Forma cruda de una fila devuelta por el modelo, antes de validar/normalizar sus campos —
     * ver {@link #normalizeRow}.
     */
    private record RawRow(
            String date,
            String description,
            BigDecimal amount,
            String movementType,
            String suggestedCategoryName
    ) {
    }
}
