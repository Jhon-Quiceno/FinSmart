package com.smartfinance.backend.ia.model.dto;

/**
 * Read-only status of a single AI provider, as exposed by {@code GET /api/ai/providers/status}
 * — never carries the provider's API key.
 *
 * @param name       the provider's display name (e.g. {@code "nvidia"})
 * @param configured whether the operator has set a non-blank API key for this provider
 * @param priority   this provider's 1-based position among enabled providers, or {@code null} if
 *                   not enabled
 */
public record AiProviderStatusResponse(String name, boolean configured, Integer priority) {
}
