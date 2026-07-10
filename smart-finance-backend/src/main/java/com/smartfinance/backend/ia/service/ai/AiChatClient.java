package com.smartfinance.backend.ia.service.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartfinance.backend.ia.exception.AiProviderAuthException;
import com.smartfinance.backend.ia.exception.AiProviderException;
import com.smartfinance.backend.ia.exception.AiProviderModelNotFoundException;
import com.smartfinance.backend.ia.exception.AiProviderRateLimitException;
import com.smartfinance.backend.ia.exception.AiProviderTimeoutException;
import com.smartfinance.backend.ia.exception.AiProviderUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Locale;

/**
 * Single OpenAI-compatible chat-completions client shared by every AI provider this project
 * supports (Gemini, NVIDIA NIM, Groq, OpenRouter, or any other provider speaking the same
 * protocol) — see {@code docs/sprints/sprint5.md}, architecture decision 2.
 *
 * <p>A fresh {@link RestClient} is built per call (via {@link RestClient.Builder#clone()}) with
 * the {@link ResolvedAiProvider#baseUrl()} of the specific provider being called. The underlying
 * {@link RestClient.Builder} bean (see {@code com.smartfinance.backend.common.config.RestClientConfig})
 * is otherwise reused as-is — in particular, this class never touches its request factory, so
 * the 10s connect / 60s read timeouts configured there survive every {@code clone()}, and a test
 * can substitute a {@code MockRestServiceServer}-bound builder without this class ever
 * overwriting the mock request factory.
 *
 * <p>Every non-2xx response or connection failure is translated into a specific
 * {@link AiProviderException} subclass (see {@link #mapResponseException} and
 * {@link #mapAccessException}) so callers never need to know about {@code RestClientException}.
 */
@Service
public class AiChatClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestClient.Builder restClientBuilder;

    public AiChatClient(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    /**
     * Sends {@code messages} to {@code provider}'s {@code /chat/completions} endpoint and
     * returns the assistant's reply.
     *
     * @param provider the AI provider configuration to call (base URL, API key and model)
     * @param messages the full conversation to send, in order (system prompt first)
     * @return the assistant's reply and, when reported, token usage
     * @throws AiProviderException if the provider rejects the request or cannot be reached
     */
    public ChatCompletionResult complete(ResolvedAiProvider provider, List<ChatMessage> messages) {
        RestClient client = buildClient(provider.baseUrl());
        ChatCompletionRequest requestBody = new ChatCompletionRequest(
                provider.model(),
                messages.stream().map(message -> new OpenAiMessage(message.role(), message.content())).toList(),
                false
        );

        try {
            ChatCompletionResponse response = client.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + provider.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            return toResult(provider.name(), response);
        } catch (RestClientResponseException ex) {
            throw mapResponseException(provider.name(), ex);
        } catch (ResourceAccessException ex) {
            throw mapAccessException(provider.name(), ex);
        }
    }

    private RestClient buildClient(String baseUrl) {
        return restClientBuilder.clone()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .build();
    }

    /**
     * Strips a single trailing slash from {@code baseUrl}, since this class always appends
     * {@link #CHAT_COMPLETIONS_PATH} with its own leading slash — providers are configured with
     * or without a trailing slash inconsistently (e.g. Gemini's OpenAI-compatible base URL ends
     * with one, Groq's does not).
     */
    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * A 200 response with no choices (e.g. a safety-filtered Gemini reply, or a provider
     * returning an unexpected empty body) is treated as a provider failure — not a bug in this
     * client — so it triggers {@link com.smartfinance.backend.ia.service.ai.AiChatOrchestrator}'s
     * failover to the next configured provider instead of escaping as an uncaught exception.
     */
    private static ChatCompletionResult toResult(String providerName, ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new AiProviderUnavailableException(providerName);
        }
        String content = response.choices().get(0).message().content();
        ChatCompletionResponse.Usage usage = response.usage();
        Integer promptTokens = usage != null ? usage.promptTokens() : null;
        Integer completionTokens = usage != null ? usage.completionTokens() : null;
        return new ChatCompletionResult(content, null, null, promptTokens, completionTokens);
    }

    /**
     * Maps an HTTP-level failure response (4xx/5xx) to the matching {@link AiProviderException}
     * subclass. Package-private so {@code AiChatClientTest} can exercise edge cases directly
     * without needing a full {@link RestClient} round trip for each one.
     */
    AiProviderException mapResponseException(String providerName, RestClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED) || status.isSameCodeAs(HttpStatus.FORBIDDEN)) {
            return new AiProviderAuthException(providerName);
        }
        if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            return new AiProviderRateLimitException(providerName);
        }
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND) || bodyMentionsModel(ex.getResponseBodyAsString())) {
            return new AiProviderModelNotFoundException(providerName);
        }
        return new AiProviderUnavailableException(providerName);
    }

    /**
     * Maps a connection-level failure (no HTTP response at all) to the matching
     * {@link AiProviderException} subclass, distinguishing a timeout from every other
     * connectivity failure (connection refused, DNS failure, etc.) by inspecting the cause.
     */
    AiProviderException mapAccessException(String providerName, ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
            return new AiProviderTimeoutException(providerName);
        }
        return new AiProviderUnavailableException(providerName);
    }

    private static boolean bodyMentionsModel(String responseBody) {
        return responseBody != null && responseBody.toLowerCase(Locale.ROOT).contains("model");
    }

    private record ChatCompletionRequest(String model, List<OpenAiMessage> messages, boolean stream) {
    }

    private record OpenAiMessage(String role, String content) {
    }

    private record ChatCompletionResponse(List<Choice> choices, Usage usage) {

        private record Choice(OpenAiMessage message) {
        }

        private record Usage(
                @JsonProperty("prompt_tokens") Integer promptTokens,
                @JsonProperty("completion_tokens") Integer completionTokens
        ) {
        }
    }
}
