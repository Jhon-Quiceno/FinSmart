package com.smartfinance.backend.ia.service.ai;

import com.smartfinance.backend.ia.exception.AiProviderException;
import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.ia.exception.AiProvidersExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Transparent failover across every configured AI provider, in priority order.
 *
 * <p>Every {@code AiChatService}/{@code AiInsightService}/{@code AiCategorizationService} call
 * goes through {@link #complete(List)} instead of resolving a single provider and calling
 * {@link AiChatClient} directly: if the currently-tried provider fails for any reason classified
 * by {@link AiProviderException} (auth, rate limit, timeout, missing model, general
 * unavailability), the next configured provider is tried automatically, with no difference
 * visible to the end user. Only when every configured provider has failed — or none is
 * configured at all — does the caller see a terminal, generic exception (see
 * {@link #GENERIC_MESSAGE}); which provider failed or why is never leaked to the caller (see
 * {@code docs/sprints/sprint5.md}).
 */
@Service
public class AiChatOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AiChatOrchestrator.class);

    /**
     * Neutral, non-actionable message shown to the end user whenever the assistant could not
     * produce a reply — reused verbatim by {@code GlobalExceptionHandler} so this exact text is
     * defined in exactly one place.
     */
    public static final String GENERIC_MESSAGE =
            "El asistente no está disponible en este momento. Inténtalo de nuevo más tarde.";

    private final AiProviderRegistry registry;
    private final AiChatClient aiChatClient;

    public AiChatOrchestrator(AiProviderRegistry registry, AiChatClient aiChatClient) {
        this.registry = registry;
        this.aiChatClient = aiChatClient;
    }

    /**
     * Sends {@code messages} to the first configured provider that succeeds, trying every
     * configured provider in priority order before giving up.
     *
     * @param messages the full conversation to send, in order (system prompt first)
     * @return the successful provider's reply
     * @throws AiProviderNotConfiguredException if no provider is configured at all
     * @throws AiProvidersExhaustedException    if every configured provider failed
     */
    public ChatCompletionResult complete(List<ChatMessage> messages) {
        List<ResolvedAiProvider> providers = registry.enabledInPriorityOrder();
        if (providers.isEmpty()) {
            throw new AiProviderNotConfiguredException(GENERIC_MESSAGE);
        }

        for (ResolvedAiProvider provider : providers) {
            try {
                ChatCompletionResult result = aiChatClient.complete(provider, messages);
                return result.withProvider(provider.name(), provider.model());
            } catch (AiProviderException ex) {
                log.warn("AI provider {} failed ({}); trying next configured provider",
                        provider.name(), ex.getClass().getSimpleName());
            }
        }

        throw new AiProvidersExhaustedException(GENERIC_MESSAGE);
    }
}
