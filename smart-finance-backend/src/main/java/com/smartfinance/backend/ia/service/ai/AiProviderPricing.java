package com.smartfinance.backend.ia.service.ai;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Rough per-1000-token cost estimates for the providers in {@link SupportedAiProvider}'s catalog,
 * used by {@link AiChatOrchestrator} to fill {@code AiUsageEvent#costEstimate} for the winning
 * attempt of every {@code complete}/{@code completeVision} call made with an {@link AiCallContext}.
 *
 * <p>This is intentionally a simple, static lookup — there is no per-provider pricing table backed
 * by configuration or a database yet, since this project only just started tracking cost at all
 * (see {@code V25__add_ai_usage_event_telemetry.sql}). The paid-provider prices below are
 * placeholders, not verified against either provider's actual current pricing page or invoice;
 * revisit them once real spend can be reconciled against these estimates.
 *
 * @see AiUsageEventService#recordAttempt
 */
@Component
public class AiProviderPricing {

    private static final BigDecimal THOUSAND_TOKENS = BigDecimal.valueOf(1000);

    /**
     * Placeholder price for NVIDIA's paid NIM endpoints — NOT verified against NVIDIA's current
     * pricing page; adjust once real pricing is confirmed.
     */
    private static final BigDecimal NVIDIA_PRICE_PER_1000_TOKENS = BigDecimal.valueOf(0.002);

    /**
     * Placeholder price for Groq's paid tier — Groq has no API key configured in this project yet
     * (see {@link SupportedAiProvider#GROQ}), so this is a reasonable guess, NOT verified against
     * Groq's current pricing page; adjust once the integration is actually activated and real
     * pricing is confirmed.
     */
    private static final BigDecimal GROQ_PRICE_PER_1000_TOKENS = BigDecimal.valueOf(0.001);

    /**
     * @param providerKey the provider's {@link SupportedAiProvider#key()} (e.g. {@code "nvidia"})
     * @param model        the exact model identifier actually used for the call (see
     *                     {@code ResolvedAiProvider#model()})
     * @param tokensUsed   total tokens consumed (prompt + completion) by the call
     * @return {@link BigDecimal#ZERO} for a known free model (OpenCode's default free model, or
     *         any model following the {@code :free} suffix convention) or when
     *         {@code tokensUsed <= 0};
     *         the estimated cost, rounded to 6 decimal places (matching
     *         {@code AiUsageEvent#costEstimate}'s column precision), for a known paid provider
     *         (NVIDIA, Groq); or {@code null} when this provider/model combination has no known
     *         pricing at all
     */
    public BigDecimal estimateCost(String providerKey, String model, int tokensUsed) {
        if (tokensUsed <= 0) {
            return BigDecimal.ZERO;
        }
        if (isKnownFreeModel(providerKey, model)) {
            return BigDecimal.ZERO;
        }
        BigDecimal pricePerThousandTokens = pricePerThousandTokens(providerKey);
        if (pricePerThousandTokens == null) {
            return null;
        }
        return pricePerThousandTokens
                .multiply(BigDecimal.valueOf(tokensUsed))
                .divide(THOUSAND_TOKENS, 6, RoundingMode.HALF_UP);
    }

    /**
     * @return {@code true} for OpenCode's known free default model, or any model following
     *         OpenRouter's {@code :free} suffix convention (case-insensitive) — both are the only
     *         confirmed-free models in this project's catalog today (see
     *         {@code docs/sprints/sprint5.md} and {@link SupportedAiProvider})
     */
    private static boolean isKnownFreeModel(String providerKey, String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        if (model.toLowerCase(Locale.ROOT).endsWith(":free")) {
            return true;
        }
        return SupportedAiProvider.OPENCODE.key().equals(providerKey)
                && SupportedAiProvider.OPENCODE.defaultModel().equals(model);
    }

    /**
     * @return the known placeholder price for a paid provider (NVIDIA, Groq), or {@code null} for
     *         OpenCode/OpenRouter non-free models (no pricing data at all yet) or any unrecognized
     *         {@code providerKey}
     */
    private static BigDecimal pricePerThousandTokens(String providerKey) {
        if (SupportedAiProvider.NVIDIA.key().equals(providerKey)) {
            return NVIDIA_PRICE_PER_1000_TOKENS;
        }
        if (SupportedAiProvider.GROQ.key().equals(providerKey)) {
            return GROQ_PRICE_PER_1000_TOKENS;
        }
        return null;
    }
}
