package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.ia.model.dto.AiUsageResponse;
import com.smartfinance.backend.ia.model.dto.ChatMessageResponse;
import com.smartfinance.backend.ia.model.dto.ChatReplyResponse;
import com.smartfinance.backend.ia.model.dto.ChatRequest;
import com.smartfinance.backend.ia.exception.AiMessageQuotaExceededException;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.ia.mapper.AiMessageMapper;
import com.smartfinance.backend.ia.model.entity.AiMessage;
import com.smartfinance.backend.ia.model.entity.AiMessageKind;
import com.smartfinance.backend.ia.model.entity.AiMessageRole;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.usuario.model.entity.User;
import com.smartfinance.backend.ia.repository.AiMessageRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.AiProviderProperties;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import com.smartfinance.backend.ia.service.ai.ChatMessage;
import com.smartfinance.backend.ia.service.ai.FinancialContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    /**
     * How many of the most recent {@link AiMessageKind#CHAT} turns are replayed to the provider
     * for conversation continuity, in addition to the new user message. Kept small since every
     * call already pays for the full financial-context system prompt, and the free tiers this
     * project targets (see {@code docs/sprints/sprint5.md}) are token- and rate-limited.
     */
    private static final int HISTORY_WINDOW_SIZE = 10;

    /**
     * Formats the quota-reset date in the exact Spanish wording shown to end users, e.g.
     * {@code "1 de agosto de 2026"}.
     */
    private static final DateTimeFormatter SPANISH_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));

    private final AiMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiChatOrchestrator aiChatOrchestrator;
    private final FinancialContextBuilder contextBuilder;
    private final AiMessageMapper aiMessageMapper;
    private final AiProviderProperties aiProviderProperties;
    private final AiUsageEventService aiUsageEventService;
    private final Clock clock;

    public AiChatService(
            AiMessageRepository messageRepository,
            UserRepository userRepository,
            AiChatOrchestrator aiChatOrchestrator,
            FinancialContextBuilder contextBuilder,
            AiMessageMapper aiMessageMapper,
            AiProviderProperties aiProviderProperties,
            AiUsageEventService aiUsageEventService,
            Clock clock
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiChatOrchestrator = aiChatOrchestrator;
        this.contextBuilder = contextBuilder;
        this.aiMessageMapper = aiMessageMapper;
        this.aiProviderProperties = aiProviderProperties;
        this.aiUsageEventService = aiUsageEventService;
        this.clock = clock;
    }

    /**
     * Reserves quota, then calls the AI provider and persists both turns, all inside the same
     * transaction as {@link #reserveMonthlyQuota}.
     *
     * <p><b>Design note on quota correctness under provider failure:</b> the reserve, the provider
     * call, and the persistence of both turns all run inside this single {@code @Transactional}
     * method sharing one database transaction. If {@link AiChatOrchestrator#complete} (or the
     * persistence that follows it) throws, the exception propagates out of this method, Spring
     * rolls back the whole transaction, and the {@code UPDATE} issued by
     * {@link UserRepository#reserveAiChatQuota} is undone along with everything else — no manual
     * refund call is needed. The tradeoff is that the reservation effectively holds the user's row
     * "locked" (in the sense that the update is uncommitted) for as long as the provider call takes
     * (up to ~180s per {@code AiChatClient}), which serializes that single user's own concurrent
     * chat requests against each other. That serialization is acceptable, and actually desirable:
     * it is exactly what closes the check-then-act race this method replaces, where two concurrent
     * requests could both observe "under limit" and both proceed. A separate reserve/refund pair
     * across two transactions was considered and rejected as needless complexity here, since it
     * would require the refund to run in a new, independent transaction (the original one is
     * already rolling back) purely to reproduce what a single transaction gives for free.
     */
    @Transactional
    public ChatReplyResponse chat(ChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        reserveMonthlyQuota(userId);

        List<ChatMessage> conversation = buildConversation(userId, request.message());
        ChatCompletionResult result = aiChatOrchestrator.complete(conversation);
        aiUsageEventService.record(
                userId, result.providerName(), AiUsageEventType.CHAT, totalTokens(result), null
        );

        persistTurn(userId, AiMessageRole.USER, request.message(), null, null);
        AiMessage assistantMessage = persistTurn(
                userId, AiMessageRole.ASSISTANT, result.content(), result.providerName(), result.model()
        );

        return new ChatReplyResponse(
                result.content(), result.providerName(), result.model(), assistantMessage.getCreatedAt()
        );
    }

    /**
     * Reports the current user's AI chat quota usage for the ongoing UTC calendar month (see
     * {@code app.ai.monthly-message-limit}), for {@code GET /api/ai/chat/usage}. Reads the
     * dedicated {@code users.ai_chat_used}/{@code ai_chat_period} counter (see {@link User}) rather
     * than counting {@code ai_messages} rows, so this stays accurate across the login-time chat
     * history purge (see {@code UserService#login}).
     */
    @Transactional(readOnly = true)
    public AiUsageResponse getUsage() {
        Long userId = SecurityUtils.getCurrentUserId();
        int limit = aiProviderProperties.getMonthlyMessageLimit();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        int used = currentUtcPeriod().equals(user.getAiChatPeriod()) ? user.getAiChatUsed() : 0;
        int remaining = Math.max(0, limit - used);
        Instant resetsAt = firstDayOfNextUtcMonth().atStartOfDay(ZoneOffset.UTC).toInstant();

        return new AiUsageResponse(used, limit, remaining, resetsAt);
    }

    /**
     * Atomically reserves one unit of the current user's monthly AI chat quota (see
     * {@link UserRepository#reserveAiChatQuota}) before any AI provider is contacted, blocking the
     * request once {@code app.ai.monthly-message-limit} has already been reserved for the current
     * UTC calendar month.
     */
    private void reserveMonthlyQuota(Long userId) {
        int limit = aiProviderProperties.getMonthlyMessageLimit();
        String period = currentUtcPeriod();
        int rowsUpdated = userRepository.reserveAiChatQuota(userId, period, limit);

        if (rowsUpdated == 0) {
            int used = userRepository.findById(userId).map(User::getAiChatUsed).orElse(limit);
            log.warn("ai_chat_quota_rejected userId={} used={} limit={}", userId, used, limit);

            String resetDate = SPANISH_DATE_FORMATTER.format(firstDayOfNextUtcMonth());
            throw new AiMessageQuotaExceededException(
                    "Alcanzaste el límite de %d mensajes de IA este mes. Tu límite se reinicia el %s."
                            .formatted(limit, resetDate)
            );
        }
    }

    /** Current UTC calendar month formatted {@code "YYYY-MM"}, matching {@code users.ai_chat_period}. */
    private String currentUtcPeriod() {
        return YearMonth.now(clock.withZone(ZoneOffset.UTC)).toString();
    }

    private LocalDate firstDayOfNextUtcMonth() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC))
                .withDayOfMonth(1)
                .plusMonths(1);
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

    /** Sums {@code promptTokens + completionTokens}, treating either as {@code 0} when the provider did not report it. */
    private static int totalTokens(ChatCompletionResult result) {
        int prompt = result.promptTokens() != null ? result.promptTokens() : 0;
        int completion = result.completionTokens() != null ? result.completionTokens() : 0;
        return prompt + completion;
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
