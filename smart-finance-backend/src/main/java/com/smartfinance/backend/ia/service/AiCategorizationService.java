package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.ia.model.dto.CategorizeRequest;
import com.smartfinance.backend.ia.model.dto.CategorizeResponse;
import com.smartfinance.backend.ia.model.dto.MovementClassification;
import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.repository.CategoryRepository;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.service.ai.AiCallContext;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import com.smartfinance.backend.ia.service.ai.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic for {@code POST /api/ai/categorize}: asks the current user's configured AI
 * provider to pick the best-matching category, from the user's own category list of the
 * requested {@link CategoryType} (income or expense), for a free-text description.
 *
 * <p>The prompt constrains the model to answer with exactly one category name from the provided
 * list, or the literal token {@value #NO_MATCH_TOKEN} when none fits — {@link #matchCategory}
 * then parses that response defensively (trimmed, case-insensitive, tolerant of surrounding
 * punctuation or extra chatter) since a free-text model reply is never fully guaranteed to be
 * exactly the instructed format.
 *
 * <p>{@link #classifyMovement} is a separate entry point for callers that don't know the
 * movement's type in advance (e.g. {@code TelegramExpenseService}): a single AI call both decides
 * the type and suggests a category, following the same defensive-parsing philosophy as
 * {@code StatementAiExtractionService}.
 */
@Service
public class AiCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(AiCategorizationService.class);

    private static final String NO_MATCH_TOKEN = "NINGUNA";

    private final CategoryRepository categoryRepository;
    private final AiChatOrchestrator aiChatOrchestrator;
    private final ObjectMapper objectMapper;

    public AiCategorizationService(
            CategoryRepository categoryRepository,
            AiChatOrchestrator aiChatOrchestrator,
            ObjectMapper objectMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.aiChatOrchestrator = aiChatOrchestrator;
        this.objectMapper = objectMapper;
    }

    /**
     * @return the matched category's id/name, or both {@code null} when the user has no
     *         categories of the requested {@link CategorizeRequest#type()} yet or the assistant
     *         found no suitable match — never throws for "no match", only for
     *         provider/configuration failures (see {@link AiChatOrchestrator#complete})
     */
    @Transactional
    public CategorizeResponse categorize(CategorizeRequest request) {
        return categorize(SecurityUtils.getCurrentUserId(), request);
    }

    /**
     * Same as {@link #categorize(CategorizeRequest)} but for a caller that already resolved
     * {@code userId} itself (e.g. {@code StatementImportService}, Sprint 2 Frente 2, which
     * categorizes several movements for the same user in one request and would otherwise
     * re-read the security context on every row).
     *
     * <p>Sin {@code @Transactional} propio a propósito: todos los llamadores actuales (por ahora,
     * {@code TelegramExpenseService}) ya están dentro de su propia transacción y envuelven esta
     * llamada en un try/catch para degradar con gracia si la IA falla. Si este método tuviera su
     * propia anotación, cualquier excepción que escape de él marcaría la transacción compartida
     * como rollback-only en el interceptor de Spring — incluso atrapada por el llamador, el commit
     * final revienta con {@code UnexpectedRollbackException} (mismo problema de fondo que el bug
     * de {@code readOnly = true} encontrado antes hoy — ver {@code StatementImportService.preview}
     * — pero disparado por cruzar el límite de un proxy @Transactional anidado en vez de por un
     * INSERT en modo lectura; ver el mismo fix en {@code ReceiptExtractionService.extractFromImage}).
     * Si en el futuro este método gana un llamador que lo invoca SIN una transacción ya abierta,
     * hay que revisar si necesita su propia anotación en ESE punto de entrada, no acá.
     */
    public CategorizeResponse categorize(Long userId, CategorizeRequest request) {
        List<Category> categories = categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(userId, request.type());
        if (categories.isEmpty()) {
            return new CategorizeResponse(null, null);
        }

        List<ChatMessage> messages = List.of(
                ChatMessage.system(buildInstruction(categories, request.type())),
                ChatMessage.user(buildUserPrompt(request))
        );
        ChatCompletionResult result = aiChatOrchestrator.complete(
                messages, new AiCallContext(userId, AiUsageEventType.CATEGORIZE)
        );

        return matchCategory(result.content(), categories)
                .map(category -> new CategorizeResponse(category.getId(), category.getName()))
                .orElseGet(() -> new CategorizeResponse(null, null));
    }

    /**
     * Decides, with a single AI call, whether a free-text movement is an {@link CategoryType#INCOME}
     * or an {@link CategoryType#EXPENSE}, and (best-effort) which of the user's categories of that
     * decided type best matches it. Used by {@code TelegramExpenseService}, where the free-text
     * message never states its own type explicitly.
     *
     * <p>Unlike {@link #categorize(Long, CategorizeRequest)}, this method never throws for a
     * malformed model response: any parsing failure degrades to
     * {@code new MovementClassification(CategoryType.EXPENSE, null, null)} (EXPENSE because that's
     * what free-text quick-add already assumes, and it's the more common case for casual
     * messages), logging a {@code WARN} with {@code userId} but never the message content. It still
     * lets provider/configuration failures from {@link AiChatOrchestrator#complete} propagate,
     * exactly like {@link #categorize(Long, CategorizeRequest)} does, so the caller can apply its
     * own fallback policy for those.
     *
     * @param userId      the user the movement belongs to
     * @param description free-text description of the movement
     * @param amount      the movement's amount
     * @return the decided type and, if matched, the category id/name
     *
     * <p>Sin {@code @Transactional} propio, mismo motivo que {@link #categorize(Long, CategorizeRequest)}.
     */
    public MovementClassification classifyMovement(Long userId, String description, BigDecimal amount) {
        List<Category> incomeCategories = categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(userId, CategoryType.INCOME);
        List<Category> expenseCategories = categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(userId, CategoryType.EXPENSE);

        List<ChatMessage> messages = List.of(
                ChatMessage.system(buildClassificationInstruction(incomeCategories, expenseCategories)),
                ChatMessage.user(buildClassificationUserPrompt(description, amount))
        );
        ChatCompletionResult result = aiChatOrchestrator.complete(
                messages, new AiCallContext(userId, AiUsageEventType.CATEGORIZE)
        );

        return parseClassification(result.content(), userId, incomeCategories, expenseCategories);
    }

    private static String buildClassificationInstruction(List<Category> incomeCategories, List<Category> expenseCategories) {
        String incomeNames = joinNames(incomeCategories);
        String expenseNames = joinNames(expenseCategories);
        return "Eres un clasificador de movimientos financieros personales. A partir de una descripción de texto "
                + "libre y un monto, debes decidir si el movimiento es un INGRESO (dinero que entra a la cuenta: "
                + "salario, pago recibido, reembolso, regalo, ingreso freelance, etc.) o un GASTO (dinero que sale: "
                + "compra, pago, servicio, etc.). La descripción puede venir recortada (sin el monto) y en frases "
                + "cortas o coloquiales: prestá atención a quién recibe el dinero. Ejemplos: \"me enviaron\", "
                + "\"me pagaron\", \"me llegó\", \"recibí\", \"me devolvieron\" indican que el dinero ENTRA (INGRESO); "
                + "\"pagué\", \"le envié\", \"compré\", \"gasté\", \"le di\" indican que el dinero SALE (GASTO). Ante "
                + "una frase ambigua sin verbo claro (ej. solo un nombre de comercio), asumí GASTO por defecto. "
                + "Responde EXCLUSIVAMENTE con un objeto JSON, sin bloques de código markdown ni texto adicional, "
                + "con esta forma exacta: {\"movementType\":\"INCOME\"|\"EXPENSE\",\"categoryName\":\"texto\"|null}. "
                + "El campo categoryName debe ser EXCLUSIVAMENTE uno de los siguientes nombres, según el tipo de "
                + "movimiento que decidiste, o null si ninguno corresponde. Categorías de ingreso disponibles: "
                + incomeNames + ". Categorías de gasto disponibles: " + expenseNames + ".";
    }

    private static String buildClassificationUserPrompt(String description, BigDecimal amount) {
        return "Descripción: " + description + ". Monto: " + amount + ".";
    }

    private static String joinNames(List<Category> categories) {
        if (categories.isEmpty()) {
            return "ninguna";
        }
        return categories.stream().map(Category::getName).collect(Collectors.joining(", "));
    }

    /**
     * Parses the model's raw JSON reply into a {@link MovementClassification}, degrading
     * gracefully (see {@link #classifyMovement}) on any malformed JSON or unrecognized
     * {@code movementType} value.
     */
    private MovementClassification parseClassification(
            String rawResponse,
            Long userId,
            List<Category> incomeCategories,
            List<Category> expenseCategories
    ) {
        RawClassification raw;
        try {
            String json = extractJsonObject(rawResponse);
            raw = objectMapper.readValue(json, RawClassification.class);
        } catch (JacksonException | IllegalArgumentException ex) {
            log.warn("telegram_classify_movement_parse_failed userId={}", userId);
            return new MovementClassification(CategoryType.EXPENSE, null, null);
        }

        CategoryType type;
        try {
            type = CategoryType.valueOf(
                    (raw.movementType() == null ? "" : raw.movementType().trim()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("telegram_classify_movement_type_invalid userId={}", userId);
            return new MovementClassification(CategoryType.EXPENSE, null, null);
        }

        List<Category> candidates = type == CategoryType.INCOME ? incomeCategories : expenseCategories;
        Category matched = matchCategoryByName(raw.categoryName(), candidates);
        return new MovementClassification(
                type,
                matched != null ? matched.getId() : null,
                matched != null ? matched.getName() : null
        );
    }

    /**
     * Resolves a category name against a candidate list, exact match first (case-insensitive),
     * then falling back to the longest candidate name that appears as a substring — the same
     * two-step approach as {@link #matchCategory}, simplified because here {@code categoryName}
     * is already an isolated JSON field rather than a free-text reply that may contain chatter.
     */
    private static Category matchCategoryByName(String categoryName, List<Category> candidates) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        String normalized = categoryName.trim();

        Optional<Category> exactMatch = candidates.stream()
                .filter(category -> category.getName().equalsIgnoreCase(normalized))
                .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }

        String lowerCaseName = normalized.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .sorted(Comparator.comparingInt((Category category) -> category.getName().length()).reversed())
                .filter(category -> lowerCaseName.contains(category.getName().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Cleans the model's raw response the same way {@code StatementAiExtractionService} does for
     * its JSON array: trims, strips an optional markdown code fence ({@code ```json ... ```} or
     * {@code ``` ... ```}), and keeps the substring between the first {@code {} and the last
     * {@code }}, tolerating extra text around the JSON object even though the prompt forbids it.
     *
     * @throws IllegalArgumentException if no JSON object could be located in the response
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
     * Raw shape of the model's classification reply, before validating/resolving its fields — see
     * {@link #parseClassification}.
     */
    private record RawClassification(String movementType, String categoryName) {
    }

    private static String buildInstruction(List<Category> categories, CategoryType type) {
        String categoryList = categories.stream().map(Category::getName).collect(Collectors.joining(", "));
        String movementNoun = type == CategoryType.INCOME ? "un ingreso" : "un gasto";
        return "Eres un clasificador de movimientos financieros personales. Debes elegir la categoría que mejor "
                + "corresponde a la descripción de " + movementNoun + ", utilizando EXCLUSIVAMENTE una de las "
                + "siguientes categorías: " + categoryList + ". Responde ÚNICAMENTE con el nombre exacto de la "
                + "categoría elegida, sin texto adicional, comillas ni explicación. Si ninguna categoría "
                + "corresponde, responde exactamente \"" + NO_MATCH_TOKEN + "\".";
    }

    private static String buildUserPrompt(CategorizeRequest request) {
        String movementLabel = request.type() == CategoryType.INCOME ? "Descripción del ingreso" : "Descripción del gasto";
        StringBuilder prompt = new StringBuilder(movementLabel).append(": \"").append(request.description()).append('"');
        if (request.amount() != null) {
            prompt.append(". Monto: $").append(request.amount());
        }
        return prompt.toString();
    }

    private static Optional<Category> matchCategory(String rawResponse, List<Category> categories) {
        if (rawResponse == null) {
            return Optional.empty();
        }
        String normalized = stripQuotesAndPunctuation(rawResponse.trim());
        if (normalized.equalsIgnoreCase(NO_MATCH_TOKEN)) {
            return Optional.empty();
        }

        Optional<Category> exactMatch = categories.stream()
                .filter(category -> category.getName().equalsIgnoreCase(normalized))
                .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        String lowerCaseResponse = rawResponse.toLowerCase(Locale.ROOT);
        return categories.stream()
                .sorted(Comparator.comparingInt((Category category) -> category.getName().length()).reversed())
                .filter(category -> lowerCaseResponse.contains(category.getName().toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    private static String stripQuotesAndPunctuation(String value) {
        return value.replaceAll("^[\"'.\\s]+|[\"'.\\s]+$", "");
    }
}
