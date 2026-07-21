package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.repository.CategoryRepository;
import com.smartfinance.backend.ia.model.dto.ReceiptExtraction;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import com.smartfinance.backend.ia.service.ai.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Extrae los datos de un recibo/comprobante de compra a partir de una imagen, usando un modelo de
 * IA con capacidad de visión, para el bot de Telegram ({@code TelegramExpenseService#registerFromPhoto}).
 *
 * <p>Sigue el mismo patrón defensivo que {@code AiCategorizationService#classifyMovement} y
 * {@code StatementAiExtractionService#extract}: construye un prompt de sistema que restringe
 * estrictamente el formato de respuesta a JSON, llama al proveedor de IA y parsea la respuesta de
 * forma tolerante, ya que un modelo de lenguaje nunca garantiza al 100% cumplir el formato
 * instruido.
 *
 * <p>A diferencia de esos dos servicios, la llamada al proveedor de IA se hace exclusivamente a
 * través de {@link AiChatOrchestrator#completeVision}, no de {@link AiChatOrchestrator#complete}:
 * este servicio necesita un modelo con capacidad de visión, y el failover multi-proveedor normal
 * podría terminar llamando a un modelo que no puede leer imágenes (ver el Javadoc de
 * {@code completeVision} para el detalle completo). El modelo de visión configurado en NVIDIA para
 * este proyecto ({@code nvidia/nemotron-nano-12b-v2-vl}) fue validado empíricamente contra un
 * recibo real: extrajo correctamente comercio, monto y tipo de movimiento, y respondió
 * {@code isReceipt: false} ante una foto sin relación.
 *
 * <p>Cualquier falla al llamar al proveedor de IA (sin configurar, agotado, timeout, etc.) se deja
 * propagar tal cual, igual que {@code classifyMovement}: quien llama a este servicio (por ejemplo,
 * {@code TelegramExpenseService}) decide cómo degradar esa falla. Solo una respuesta del modelo que
 * no se pudo interpretar como JSON, o que el propio modelo marca como "no es un recibo", degrada
 * internamente al resultado {@link ReceiptExtraction#notAReceipt()} — nunca lanza una excepción por
 * eso, porque una foto ilegible o irrelevante es un resultado esperado, no una falla del proveedor.
 */
@Service
public class ReceiptExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptExtractionService.class);

    private static final String INSTRUCTION_TEXT = "Extraé los datos de este recibo.";

    private final CategoryRepository categoryRepository;
    private final AiChatOrchestrator aiChatOrchestrator;
    private final AiUsageEventService aiUsageEventService;
    private final ObjectMapper objectMapper;

    public ReceiptExtractionService(
            CategoryRepository categoryRepository,
            AiChatOrchestrator aiChatOrchestrator,
            AiUsageEventService aiUsageEventService,
            ObjectMapper objectMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.aiChatOrchestrator = aiChatOrchestrator;
        this.aiUsageEventService = aiUsageEventService;
        this.objectMapper = objectMapper;
    }

    /**
     * @param userId   el usuario dueño de las categorías contra las que se intenta resolver
     *                 {@code categoryName} (se resuelven internamente, el llamador no necesita
     *                 obtenerlas de antemano)
     * @param imageUrl la imagen del recibo, como URL {@code https://} o data URI
     *                 {@code data:image/...;base64,...} (ambas formas son aceptadas tal cual por el
     *                 campo {@code image_url.url} del proveedor)
     * @return los datos extraídos, o {@link ReceiptExtraction#notAReceipt()} cuando la imagen no
     *         parece un recibo real, o su monto/descripción no son utilizables
     * @throws com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException si NVIDIA no
     *         está configurado (ver {@link AiChatOrchestrator#completeVision})
     * @throws com.smartfinance.backend.ia.exception.AiProviderException si NVIDIA rechaza la
     *         solicitud o no se pudo alcanzar
     *
     * <p>Sin {@code @Transactional} propio a propósito: este método siempre se llama desde
     * adentro de otro método ya transaccional ({@code TelegramExpenseService.registerFromPhoto}),
     * que lo envuelve en un try/catch para degradar con gracia si la IA falla. Si este método
     * tuviera su propia anotación, cualquier excepción que escape de él marcaría la transacción
     * compartida como rollback-only en el interceptor de Spring — incluso atrapada por el
     * llamador, el commit final revienta con {@code UnexpectedRollbackException} (mismo problema
     * de fondo que el bug de {@code readOnly = true} encontrado antes hoy, pero disparado por
     * cruzar el límite de un proxy @Transactional anidado en vez de por un INSERT en modo lectura).
     */
    public ReceiptExtraction extractFromImage(Long userId, String imageUrl) {
        List<Category> incomeCategories = categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(userId, CategoryType.INCOME);
        List<Category> expenseCategories = categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(userId, CategoryType.EXPENSE);

        List<ChatMessage> messages = List.of(
                ChatMessage.system(buildInstruction(incomeCategories, expenseCategories)),
                ChatMessage.userWithImage(INSTRUCTION_TEXT, imageUrl)
        );
        ChatCompletionResult result = aiChatOrchestrator.completeVision(messages);
        aiUsageEventService.record(userId, result.providerName(), AiUsageEventType.CATEGORIZE, totalTokens(result), null);

        return parseExtraction(result.content(), userId, incomeCategories, expenseCategories);
    }

    private static String buildInstruction(List<Category> incomeCategories, List<Category> expenseCategories) {
        String incomeNames = joinNames(incomeCategories);
        String expenseNames = joinNames(expenseCategories);
        return "Sos un extractor de datos de recibos y facturas de compra. A partir de una imagen, identificá "
                + "si se trata de un recibo o comprobante de compra real y legible. Si lo es, extraé el nombre "
                + "del comercio o una descripción breve, el monto TOTAL de la operación (no una línea individual "
                + "del detalle), y decidí si el movimiento es un INGRESO (poco común: solo para notas de crédito "
                + "o reembolsos) o un GASTO (lo habitual para un recibo de compra). También elegí la categoría "
                + "que mejor corresponda de la lista del tipo de movimiento que decidiste, o null si ninguna "
                + "corresponde. Si la imagen no parece un recibo o comprobante real (por ejemplo, una foto sin "
                + "relación), respondé con isReceipt en false y el resto de los campos en null. Responde "
                + "EXCLUSIVAMENTE con un objeto JSON, sin bloques de código markdown ni texto adicional, con "
                + "esta forma exacta: {\"isReceipt\":true|false,\"description\":\"texto\"|null,\"amount\":numero|null,"
                + "\"movementType\":\"INCOME\"|\"EXPENSE\"|null,\"categoryName\":\"texto\"|null}. El campo "
                + "categoryName debe ser EXCLUSIVAMENTE uno de los siguientes nombres, según el tipo de "
                + "movimiento que decidiste, o null si ninguno corresponde. Categorías de ingreso disponibles: "
                + incomeNames + ". Categorías de gasto disponibles: " + expenseNames + ".";
    }

    private static String joinNames(List<Category> categories) {
        if (categories.isEmpty()) {
            return "ninguna";
        }
        return categories.stream().map(Category::getName).collect(Collectors.joining(", "));
    }

    /**
     * Parsea la respuesta cruda del modelo, degradando a {@link ReceiptExtraction#notAReceipt()}
     * (nunca lanzando) ante JSON mal formado, {@code isReceipt: false}, o un monto/descripción no
     * utilizables — ver la clase Javadoc.
     */
    private ReceiptExtraction parseExtraction(
            String rawResponse,
            Long userId,
            List<Category> incomeCategories,
            List<Category> expenseCategories
    ) {
        RawReceiptExtraction raw;
        try {
            String json = extractJsonObject(rawResponse);
            raw = objectMapper.readValue(json, RawReceiptExtraction.class);
        } catch (JacksonException | IllegalArgumentException ex) {
            log.warn("telegram_receipt_extraction_parse_failed userId={}", userId);
            return ReceiptExtraction.notAReceipt();
        }

        if (raw.isReceipt() == null || !raw.isReceipt()
                || raw.description() == null || raw.description().isBlank()
                || raw.amount() == null || raw.amount().signum() <= 0) {
            return ReceiptExtraction.notAReceipt();
        }

        CategoryType type = parseMovementType(raw.movementType());
        List<Category> candidates = type == CategoryType.INCOME ? incomeCategories : expenseCategories;
        Category matched = matchCategoryByName(raw.categoryName(), candidates);

        return new ReceiptExtraction(
                true,
                raw.description().trim(),
                raw.amount(),
                type,
                matched != null ? matched.getId() : null,
                matched != null ? matched.getName() : null
        );
    }

    /** El recibo de una compra es casi siempre un GASTO: ante un valor ausente o no reconocido, se asume ese caso. */
    private static CategoryType parseMovementType(String rawMovementType) {
        try {
            return CategoryType.valueOf((rawMovementType == null ? "" : rawMovementType.trim()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CategoryType.EXPENSE;
        }
    }

    /**
     * Resuelve un nombre de categoría contra una lista de candidatas, con match exacto primero
     * (sin distinguir mayúsculas/minúsculas) y, si no hay match exacto, con el nombre candidato más
     * largo que aparezca como subcadena — mismo criterio de dos pasos que
     * {@code AiCategorizationService#matchCategoryByName}.
     */
    private static Category matchCategoryByName(String categoryName, List<Category> candidates) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        String normalized = categoryName.trim();

        return candidates.stream()
                .filter(category -> category.getName().equalsIgnoreCase(normalized))
                .findFirst()
                .or(() -> candidates.stream()
                        .sorted(Comparator.comparingInt((Category category) -> category.getName().length()).reversed())
                        .filter(category -> normalized.toLowerCase(Locale.ROOT).contains(category.getName().toLowerCase(Locale.ROOT)))
                        .findFirst())
                .orElse(null);
    }

    /**
     * Limpia la respuesta cruda del modelo: recorta espacios, quita un posible bloque de código
     * markdown ({@code ```json ... ```} o {@code ``` ... ```}) y se queda con la subcadena entre el
     * primer {@code {} y el último {@code }}, tolerando texto adicional alrededor del objeto JSON
     * aunque el prompt lo prohíba explícitamente — mismo criterio que
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

    /** Sums {@code promptTokens + completionTokens}, treating either as {@code 0} when the provider did not report it. */
    private static int totalTokens(ChatCompletionResult result) {
        int prompt = result.promptTokens() != null ? result.promptTokens() : 0;
        int completion = result.completionTokens() != null ? result.completionTokens() : 0;
        return prompt + completion;
    }

    /**
     * Forma cruda de la respuesta del modelo, antes de validar/normalizar sus campos — ver
     * {@link #parseExtraction}.
     */
    private record RawReceiptExtraction(
            Boolean isReceipt,
            String description,
            BigDecimal amount,
            String movementType,
            String categoryName
    ) {
    }
}
