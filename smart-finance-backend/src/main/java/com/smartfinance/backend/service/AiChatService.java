package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.ai.ChatMessageResponse;
import com.smartfinance.backend.dto.ai.ChatReplyResponse;
import com.smartfinance.backend.dto.ai.ChatRequest;
import com.smartfinance.backend.mapper.AiMessageMapper;
import com.smartfinance.backend.model.AiMessage;
import com.smartfinance.backend.model.AiMessageKind;
import com.smartfinance.backend.model.AiMessageRole;
import com.smartfinance.backend.repository.AiMessageRepository;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.security.SecurityUtils;
import com.smartfinance.backend.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.service.ai.ChatCompletionResult;
import com.smartfinance.backend.service.ai.ChatMessage;
import com.smartfinance.backend.service.ai.FinancialContextBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Business logic for {@code POST /api/ai/chat} and {@code GET /api/ai/chat/history}.
 *
 * <p>Every call resolves the caller via {@link SecurityUtils#getCurrentUserId()}, delegates the
 * provider resolution and transparent failover to {@link AiChatOrchestrator#complete(List)}, and
 * injects the user's real financial context (see {@link FinancialContextBuilder}) as the system
 * prompt on every call — the assistant is "trained" per-request rather than fine-tuned (see
 * {@code docs/sprints/sprint5.md}, architecture decision 4).
 *
 * <p>Both the user's question and the assistant's reply are persisted as {@link AiMessage} rows
 * only after a successful {@link AiChatOrchestrator#complete} call — if every configured provider
 * fails, a terminal exception propagates and nothing is written, keeping the conversation history
 * free of half-completed turns.
 */
@Service
public class AiChatService {

    /**
     * How many of the most recent {@link AiMessageKind#CHAT} turns are replayed to the provider
     * for conversation continuity, in addition to the new user message. Kept small since every
     * call already pays for the full financial-context system prompt, and the free tiers this
     * project targets (see {@code docs/sprints/sprint5.md}) are token- and rate-limited.
     */
    private static final int HISTORY_WINDOW_SIZE = 10;

    private final AiMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiChatOrchestrator aiChatOrchestrator;
    private final FinancialContextBuilder contextBuilder;
    private final AiMessageMapper aiMessageMapper;

    public AiChatService(
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

    @Transactional
    public ChatReplyResponse chat(ChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        List<ChatMessage> conversation = buildConversation(userId, request.message());
        ChatCompletionResult result = aiChatOrchestrator.complete(conversation);

        persistTurn(userId, AiMessageRole.USER, request.message(), null, null);
        AiMessage assistantMessage = persistTurn(
                userId, AiMessageRole.ASSISTANT, result.content(), result.providerName(), result.model()
        );

        return new ChatReplyResponse(
                result.content(), result.providerName(), result.model(), assistantMessage.getCreatedAt()
        );
    }

    /**
     * Returns the current user's chat history, most-recent-first (same convention as
     * {@code GET /api/notifications}), one page at a time. This is the client-facing ordering
     * only — {@link #buildConversation} independently loads and re-chronologizes the same rows
     * for the provider call.
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getHistory(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return messageRepository.findByUser_IdAndKindOrderByCreatedAtDesc(userId, AiMessageKind.CHAT, pageable)
                .map(aiMessageMapper::toChatResponse);
    }

    private List<ChatMessage> buildConversation(Long userId, String newUserMessage) {
        String systemPrompt = contextBuilder.buildSystemPrompt();
        List<AiMessage> recentHistoryDesc = messageRepository
                .findByUser_IdAndKindOrderByCreatedAtDesc(userId, AiMessageKind.CHAT, PageRequest.of(0, HISTORY_WINDOW_SIZE))
                .getContent();

        List<AiMessage> chronological = new ArrayList<>(recentHistoryDesc);
        Collections.reverse(chronological);

        List<ChatMessage> conversation = new ArrayList<>();
        conversation.add(ChatMessage.system(systemPrompt));
        for (AiMessage message : chronological) {
            conversation.add(message.getRole() == AiMessageRole.USER
                    ? ChatMessage.user(message.getContent())
                    : ChatMessage.assistant(message.getContent()));
        }
        conversation.add(ChatMessage.user(newUserMessage));
        return conversation;
    }

    private AiMessage persistTurn(Long userId, AiMessageRole role, String content, String providerName, String model) {
        AiMessage message = new AiMessage();
        message.setUser(userRepository.getReferenceById(userId));
        message.setRole(role);
        message.setKind(AiMessageKind.CHAT);
        message.setContent(content);
        message.setProviderName(providerName);
        message.setModel(model);
        return messageRepository.save(message);
    }
}
