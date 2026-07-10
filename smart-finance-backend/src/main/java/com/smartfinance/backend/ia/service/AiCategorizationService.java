package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.ia.model.dto.CategorizeRequest;
import com.smartfinance.backend.ia.model.dto.CategorizeResponse;
import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.repository.CategoryRepository;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import com.smartfinance.backend.ia.service.ai.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic for {@code POST /api/ai/categorize}: asks the current user's configured AI
 * provider to pick the best-matching expense category, from the user's own category list, for a
 * free-text expense description.
 *
 * <p>The prompt constrains the model to answer with exactly one category name from the provided
 * list, or the literal token {@value #NO_MATCH_TOKEN} when none fits — {@link #matchCategory}
 * then parses that response defensively (trimmed, case-insensitive, tolerant of surrounding
 * punctuation or extra chatter) since a free-text model reply is never fully guaranteed to be
 * exactly the instructed format.
 */
@Service
public class AiCategorizationService {

    private static final String NO_MATCH_TOKEN = "NINGUNA";

    private final CategoryRepository categoryRepository;
    private final AiChatOrchestrator aiChatOrchestrator;

    public AiCategorizationService(
            CategoryRepository categoryRepository,
            AiChatOrchestrator aiChatOrchestrator
    ) {
        this.categoryRepository = categoryRepository;
        this.aiChatOrchestrator = aiChatOrchestrator;
    }

    /**
     * @return the matched category's id/name, or both {@code null} when the user has no expense
     *         categories yet or the assistant found no suitable match — never throws for "no
     *         match", only for provider/configuration failures (see
     *         {@link AiChatOrchestrator#complete})
     */
    @Transactional(readOnly = true)
    public CategorizeResponse categorize(CategorizeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Category> categories = categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(userId, CategoryType.EXPENSE);
        if (categories.isEmpty()) {
            return new CategorizeResponse(null, null);
        }

        List<ChatMessage> messages = List.of(
                ChatMessage.system(buildInstruction(categories)),
                ChatMessage.user(buildUserPrompt(request))
        );
        ChatCompletionResult result = aiChatOrchestrator.complete(messages);

        return matchCategory(result.content(), categories)
                .map(category -> new CategorizeResponse(category.getId(), category.getName()))
                .orElseGet(() -> new CategorizeResponse(null, null));
    }

    private static String buildInstruction(List<Category> categories) {
        String categoryList = categories.stream().map(Category::getName).collect(Collectors.joining(", "));
        return "Eres un clasificador de gastos personales. Debes elegir la categoría que mejor corresponde a la "
                + "descripción de un gasto, utilizando EXCLUSIVAMENTE una de las siguientes categorías: "
                + categoryList + ". Responde ÚNICAMENTE con el nombre exacto de la categoría elegida, sin texto "
                + "adicional, comillas ni explicación. Si ninguna categoría corresponde, responde exactamente \""
                + NO_MATCH_TOKEN + "\".";
    }

    private static String buildUserPrompt(CategorizeRequest request) {
        StringBuilder prompt = new StringBuilder("Descripción del gasto: \"").append(request.description()).append('"');
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
