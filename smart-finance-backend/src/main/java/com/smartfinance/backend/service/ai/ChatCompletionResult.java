package com.smartfinance.backend.service.ai;

/**
 * Outcome of a successful AI chat completion: the assistant's reply text, which provider actually
 * answered, and, when the provider reports it, token usage.
 *
 * <p>{@link AiChatClient#complete} does not know which provider it was called with in terms of
 * display metadata beyond what it needs for error mapping, so it returns {@code providerName} and
 * {@code model} as {@code null}; {@link AiChatOrchestrator#complete} stamps both in via
 * {@link #withProvider(String, String)} once it knows which configured provider actually
 * succeeded, before handing the result back to callers.
 *
 * @param content           the assistant's reply text
 * @param providerName      the name of the provider that produced this reply, or {@code null}
 *                          until stamped by {@link AiChatOrchestrator}
 * @param model             the model identifier used to produce this reply, or {@code null} until
 *                          stamped by {@link AiChatOrchestrator}
 * @param promptTokens      tokens consumed by the prompt, or {@code null} if the provider did
 *                          not report usage
 * @param completionTokens  tokens consumed by the completion, or {@code null} if the provider
 *                          did not report usage
 */
public record ChatCompletionResult(
        String content,
        String providerName,
        String model,
        Integer promptTokens,
        Integer completionTokens
) {

    /**
     * Returns a copy of this result with {@code providerName}/{@code model} filled in, keeping
     * every other field as-is. Used by {@link AiChatOrchestrator} once it knows which configured
     * provider actually answered.
     */
    ChatCompletionResult withProvider(String providerName, String model) {
        return new ChatCompletionResult(content, providerName, model, promptTokens, completionTokens);
    }
}
