package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.ia.model.dto.SummaryPeriod;
import com.smartfinance.backend.ia.model.dto.SummaryQueryIntent;
import com.smartfinance.backend.ia.model.dto.SummaryTopic;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.service.ai.AiCallContext;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import com.smartfinance.backend.ia.service.ai.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Locale;

/**
 * Extracts the period, movement type, and category a free-text financial-summary question refers
 * to (e.g. {@code "¿Cuánto gasté en comida este mes?"}), with a single AI call. Used by
 * {@code TelegramExpenseService} to answer a Telegram message that
 * {@code TelegramIntentDetector#looksLikeSummaryQuery} flagged as a query instead of a movement
 * registration attempt.
 *
 * <p>Follows the same strict-JSON, defensively-parsed pattern as
 * {@link AiCategorizationService#classifyMovement} and {@code ReceiptExtractionService}. Unlike
 * those two, {@link #parseQuery} never lets ANY failure propagate — not even a provider/configuration
 * failure from {@link AiChatOrchestrator#complete} — and instead degrades to
 * {@link #DEFAULT_INTENT} ({@link SummaryPeriod#MONTH}, no movement type filter, no category).
 * Registering a movement must never silently default a field the user didn't state (that would
 * corrupt real data), which is why {@code classifyMovement} lets provider failures propagate for
 * its caller to handle explicitly; a summary is a read-only reply, so defaulting to "this month,
 * everything" when the AI provider is unavailable is simply a slightly-less-precise answer, not a
 * data-integrity risk.
 *
 * <p>Sin {@code @Transactional} propio: este método siempre se llama desde adentro de la
 * transacción ya abierta por {@code TelegramExpenseService#registerFromMessage}, que la rama de
 * resumen no necesita para nada (solo hace lecturas), y agregar una anotación propia acá marcaría
 * esa transacción compartida como rollback-only ante cualquier excepción que cruce el proxy de este
 * método — el mismo problema documentado en {@link AiCategorizationService#classifyMovement}.
 */
@Service
public class FinancialSummaryQueryService {

    private static final Logger log = LoggerFactory.getLogger(FinancialSummaryQueryService.class);

    private static final SummaryQueryIntent DEFAULT_INTENT =
            new SummaryQueryIntent(SummaryPeriod.MONTH, null, null, SummaryTopic.MOVEMENT);

    private static final String INSTRUCTION =
            "Sos un asistente que interpreta preguntas en lenguaje natural sobre finanzas personales. A partir de "
                    + "una pregunta de texto libre, extraé el período, el tema, el tipo de movimiento y la "
                    + "categoría a la que se refiere. Responde EXCLUSIVAMENTE con un objeto JSON, sin bloques de "
                    + "código markdown ni texto adicional, con esta forma exacta: {\"period\":\"TODAY\"|\"WEEK\"|"
                    + "\"MONTH\"|\"LAST_MONTH\"|\"YEAR\",\"topic\":\"MOVEMENT\"|\"DEBT\","
                    + "\"movementType\":\"EXPENSE\"|\"INCOME\"|null,\"categoryName\":\"texto\"|null}. Usá "
                    + "\"TODAY\" solo si la pregunta menciona explícitamente hoy, \"WEEK\" solo si menciona "
                    + "explícitamente esta semana, \"LAST_MONTH\" solo si menciona explícitamente el mes pasado, "
                    + "\"YEAR\" solo si menciona explícitamente este año o en el año, y \"MONTH\" en cualquier "
                    + "otro caso, incluido cuando no se menciona ningún período. Ejemplos de período: "
                    + "\"resumen\" -> MONTH; \"balance\" -> MONTH; \"cuánto gasté\" (sin mencionar cuándo) -> "
                    + "MONTH; \"cuánto gasté hoy\" -> TODAY; \"cuánto gasté esta semana\" -> WEEK; \"cuánto gasté "
                    + "el mes pasado\" -> LAST_MONTH; \"cuánto gasté este año\" -> YEAR. Usá \"DEBT\" para "
                    + "preguntas sobre deudas (ej. \"¿cómo voy con mis deudas?\", \"cuánto debo\"), y "
                    + "\"MOVEMENT\" para cualquier otra pregunta sobre gastos, ingresos o balance. Usá "
                    + "\"EXPENSE\" para preguntas sobre gastos, \"INCOME\" para preguntas sobre ingresos, y null "
                    + "para preguntas de balance, resumen general, sobre deudas, o cualquier otra que no "
                    + "distinga entre ambos. El campo categoryName debe contener el nombre de la categoría "
                    + "mencionada (por ejemplo, \"comida\", \"transporte\") EXCLUSIVAMENTE si la pregunta nombra "
                    + "una categoría específica; en caso contrario respondé null. No inventes una categoría que "
                    + "no esté explícita en la pregunta.";

    private final AiChatOrchestrator aiChatOrchestrator;
    private final ObjectMapper objectMapper;

    public FinancialSummaryQueryService(
            AiChatOrchestrator aiChatOrchestrator,
            ObjectMapper objectMapper
    ) {
        this.aiChatOrchestrator = aiChatOrchestrator;
        this.objectMapper = objectMapper;
    }

    /**
     * @param userId the user asking the question, used only to attribute AI usage tracking
     * @param text   the free-text question (e.g. {@code "¿Cuánto gasté en comida este mes?"})
     * @return the decided period/movement type/category, or {@link #DEFAULT_INTENT} when the AI
     *         provider fails or its response could not be interpreted — never throws
     */
    public SummaryQueryIntent parseQuery(Long userId, String text) {
        try {
            List<ChatMessage> messages = List.of(
                    ChatMessage.system(INSTRUCTION),
                    ChatMessage.user(text)
            );
            ChatCompletionResult result = aiChatOrchestrator.complete(
                    messages, new AiCallContext(userId, AiUsageEventType.CATEGORIZE)
            );
            return parseIntent(result.content(), userId);
        } catch (RuntimeException ex) {
            log.warn("telegram_summary_query_failed userId={}", userId, ex);
            return DEFAULT_INTENT;
        }
    }

    /**
     * Parses the model's raw JSON reply into a {@link SummaryQueryIntent}, degrading to
     * {@link #DEFAULT_INTENT} on malformed JSON (never throws) — the {@code period} and
     * {@code movementType} fields are additionally each defaulted/discarded individually when
     * unrecognized, rather than failing the whole response over one bad field.
     */
    private SummaryQueryIntent parseIntent(String rawResponse, Long userId) {
        RawIntent raw;
        try {
            String json = extractJsonObject(rawResponse);
            raw = objectMapper.readValue(json, RawIntent.class);
        } catch (JacksonException | IllegalArgumentException ex) {
            log.warn("telegram_summary_query_parse_failed userId={}", userId);
            return DEFAULT_INTENT;
        }

        SummaryPeriod period = parsePeriod(raw.period());
        CategoryType movementType = parseMovementType(raw.movementType());
        String categoryName = raw.categoryName() == null || raw.categoryName().isBlank() ? null : raw.categoryName().trim();
        SummaryTopic topic = parseTopic(raw.topic());
        return new SummaryQueryIntent(period, movementType, categoryName, topic);
    }

    /** Cualquier valor no reconocido, incluido {@code null}, cae al default documentado en la clase: {@code MONTH}. */
    private static SummaryPeriod parsePeriod(String rawPeriod) {
        try {
            return SummaryPeriod.valueOf((rawPeriod == null ? "" : rawPeriod.trim()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SummaryPeriod.MONTH;
        }
    }

    /** Cualquier valor no reconocido, incluido {@code null}, cae al default documentado en la clase: {@code MOVEMENT}. */
    private static SummaryTopic parseTopic(String rawTopic) {
        try {
            return SummaryTopic.valueOf((rawTopic == null ? "" : rawTopic.trim()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SummaryTopic.MOVEMENT;
        }
    }

    /** Cualquier valor no reconocido, incluido {@code null}, cae a {@code null} (balance/ambos tipos). */
    private static CategoryType parseMovementType(String rawMovementType) {
        if (rawMovementType == null || rawMovementType.isBlank()) {
            return null;
        }
        try {
            return CategoryType.valueOf(rawMovementType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Limpia la respuesta cruda del modelo: recorta espacios, quita un posible bloque de código
     * markdown ({@code ```json ... ```} o {@code ``` ... ```}) y se queda con la subcadena entre el
     * primer {@code {} y el último {@code }} — mismo criterio que
     * {@code AiCategorizationService#extractJsonObject}.
     *
     * @throws IllegalArgumentException si no se encontró un objeto JSON en la respuesta
     */
    private static String extractJsonObject(String rawResponse) {
        if (rawResponse == null) {
            throw new IllegalArgumentException("La respuesta del modelo está vacía");
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

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("No se encontró un objeto JSON en la respuesta del modelo");
        }
        return trimmed.substring(start, end + 1);
    }

    /**
     * Raw shape of the model's reply, before validating/normalizing its fields — see
     * {@link #parseIntent}.
     */
    private record RawIntent(String period, String movementType, String categoryName, String topic) {
    }
}
