package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.ia.model.dto.InsightResponse;
import com.smartfinance.backend.ia.mapper.AiMessageMapper;
import com.smartfinance.backend.ia.model.entity.AiMessage;
import com.smartfinance.backend.ia.model.entity.AiMessageKind;
import com.smartfinance.backend.ia.model.entity.AiMessageRole;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.repository.AiMessageRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.ia.service.ai.AiCallContext;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import com.smartfinance.backend.ia.service.ai.ChatMessage;
import com.smartfinance.backend.ia.service.ai.FinancialContextBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for {@code GET /api/ai/insights} and {@code POST /api/ai/insights/generate}.
 *
 * <p>Insights are persisted as {@link AiMessageKind#INSIGHT} rows so the dashboard can show the
 * latest one without calling the AI provider on every page load, staying well under the free-tier
 * rate limits this project targets (see {@code docs/sprints/sprint5.md}, architecture decision 8)
 * — the user regenerates a new one on demand via {@link #generateInsight()}.
 */
@Service
public class AiInsightService {

    private static final String INSIGHT_INSTRUCTION =
            "Con base en el contexto financiero anterior, genera entre 3 y 5 recomendaciones financieras "
                    + "personalizadas, concretas y accionables, en español, en formato de lista con viñetas "
                    + "('- '). No repitas los datos ya provistos: concéntrate en recomendaciones.";

    private final AiMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiChatOrchestrator aiChatOrchestrator;
    private final FinancialContextBuilder contextBuilder;
    private final AiMessageMapper aiMessageMapper;

    public AiInsightService(
            AiMessageRepository messageRepository,
            UserRepository userRepository,
            AiChatOrchestrator aiChatOrchestrator,
            FinancialContextBuilder contextBuilder,
            AiMessageMapper aiMessageMapper
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiChatOrchestrator = aiChatOrchestrator;
        this.contextBuilder = contextBuilder;
        this.aiMessageMapper = aiMessageMapper;
    }

    /**
     * @return the current user's most recently generated insight, or {@code null} if none has
     *         ever been generated (the controller maps this to {@code 204 No Content})
     */
    @Transactional(readOnly = true)
    public InsightResponse getLatestInsight() {
        Long userId = SecurityUtils.getCurrentUserId();
        return messageRepository.findFirstByUser_IdAndKindOrderByCreatedAtDesc(userId, AiMessageKind.INSIGHT)
                .map(aiMessageMapper::toInsightResponse)
                .orElse(null);
    }

    @Transactional
    public InsightResponse generateInsight() {
        Long userId = SecurityUtils.getCurrentUserId();
        String systemPrompt = contextBuilder.buildSystemPrompt();

        List<ChatMessage> messages = List.of(
                ChatMessage.system(systemPrompt),
                ChatMessage.user(INSIGHT_INSTRUCTION)
        );
        ChatCompletionResult result = aiChatOrchestrator.complete(
                messages, new AiCallContext(userId, AiUsageEventType.INSIGHT)
        );

        AiMessage insight = new AiMessage();
        insight.setUser(userRepository.getReferenceById(userId));
        insight.setRole(AiMessageRole.ASSISTANT);
        insight.setKind(AiMessageKind.INSIGHT);
        insight.setContent(result.content());
        insight.setProviderName(result.providerName());
        insight.setModel(result.model());

        return aiMessageMapper.toInsightResponse(messageRepository.save(insight));
    }
}
