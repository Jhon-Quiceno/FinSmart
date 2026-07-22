package com.smartfinance.backend.ia.service.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartfinance.backend.ia.exception.AiProviderAuthException;
import com.smartfinance.backend.ia.exception.AiProviderException;
import com.smartfinance.backend.ia.exception.AiProviderModelNotFoundException;
import com.smartfinance.backend.ia.exception.AiProviderRateLimitException;
import com.smartfinance.backend.ia.exception.AiProviderTimeoutException;
import com.smartfinance.backend.ia.exception.AiProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AiChatClient.class);
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
                messages.stream().map(AiChatClient::toOpenAiMessage).toList(),
                false,
                buildChatTemplateKwargs(provider)
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
            // Temporary diagnostic logging (2026-07-22): AiProviderException subclasses intentionally
            // carry only a generic user-facing message (see their Javadoc), discarding the real cause -
            // this is the only place that detail is still available before it's lost.
            log.warn("ai_provider_http_error provider={} status={} body={}",
                    provider.name(), ex.getStatusCode(), truncate(ex.getResponseBodyAsString()));
            throw mapResponseException(provider.name(), ex);
        } catch (ResourceAccessException ex) {
            log.warn("ai_provider_connection_error provider={} message={}", provider.name(), ex.getMessage());
            throw mapAccessException(provider.name(), ex);
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return null;
        }
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    /**
     * NVIDIA's newer Nemotron-family models default to a verbose hybrid "reasoning" mode: without
     * this flag they spend the entire token budget on chain-of-thought and never emit the actual
     * JSON answer this app's prompts demand, truncating with {@code finish_reason: "length"}. It's
     * an NVIDIA-only vendor extension (not part of the OpenAI chat-completions spec), harmless to
     * send even when the configured model doesn't have a "thinking" mode at all — verified against
     * every candidate model tried for this project, including the current default. Applying it
     * unconditionally for every NVIDIA model (not just the one currently configured) means a future
     * {@code NVIDIA_MODEL} swap doesn't silently reintroduce this failure mode.
     */
    private static ChatCompletionRequest.ChatTemplateKwargs buildChatTemplateKwargs(ResolvedAiProvider provider) {
        return SupportedAiProvider.NVIDIA.key().equals(provider.name())
                ? new ChatCompletionRequest.ChatTemplateKwargs(false)
                : null;
    }

    /**
     * Converts a {@link ChatMessage} into the wire-level {@link OpenAiMessage}, choosing between
     * the two content shapes the OpenAI chat-completions contract accepts: a plain string for a
     * normal text turn (unchanged from before vision support was added, so every existing
     * plain-text caller serializes exactly as it always did), or an array of content parts
     * ({@code [{"type":"text",...},{"type":"image_url",...}]}) when {@link ChatMessage#imageUrl()}
     * is set.
     */
    private static OpenAiMessage toOpenAiMessage(ChatMessage message) {
        if (message.imageUrl() == null) {
            return new OpenAiMessage(message.role(), message.content());
        }
        return new OpenAiMessage(message.role(), List.of(
                ContentPart.text(message.content()),
                ContentPart.imageUrl(message.imageUrl())
        ));
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatCompletionRequest(
            String model,
            List<OpenAiMessage> messages,
            boolean stream,
            @JsonProperty("chat_template_kwargs") ChatTemplateKwargs chatTemplateKwargs
    ) {
        private record ChatTemplateKwargs(Boolean thinking) {
        }
    }

    /**
     * Wire-level outgoing message. {@code content} is intentionally typed {@link Object} rather
     * than {@code String}: it holds either a plain {@link String} (a normal text turn) or a
     * {@code List<ContentPart>} (a vision turn), and Jackson serializes each shape as-is — a JSON
     * string in the first case, a JSON array of {@code {"type":...}} objects in the second. See
     * {@link #toOpenAiMessage(ChatMessage)}.
     */
    private record OpenAiMessage(String role, Object content) {
    }

    /**
     * One element of the OpenAI vision {@code content} array. Only one of {@code text}/{@code imageUrl}
     * is ever set per instance (see {@link #text(String)}/{@link #imageUrl(String)}); the other is
     * omitted from the JSON via {@code @JsonInclude(NON_NULL)} so each part serializes with only the
     * field its {@code type} expects.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ContentPart(String type, String text, @JsonProperty("image_url") ImageUrlPart imageUrl) {

        private static final String TYPE_TEXT = "text";
        private static final String TYPE_IMAGE_URL = "image_url";

        private static ContentPart text(String text) {
            return new ContentPart(TYPE_TEXT, text, null);
        }

        private static ContentPart imageUrl(String url) {
            return new ContentPart(TYPE_IMAGE_URL, null, new ImageUrlPart(url));
        }

        private record ImageUrlPart(String url) {
        }
    }

    private record ChatCompletionResponse(List<Choice> choices, Usage usage) {

        /**
         * The provider's reply message is always plain text in this project (no provider we call
         * ever answers with a multimodal {@code content} array), so this response-side type keeps
         * {@code content} as a plain {@link String} — distinct from the request-side
         * {@link OpenAiMessage}, whose {@code content} must be polymorphic to support vision turns.
         */
        private record Choice(ResponseMessage message) {
        }

        private record ResponseMessage(String role, String content) {
        }

        private record Usage(
                @JsonProperty("prompt_tokens") Integer promptTokens,
                @JsonProperty("completion_tokens") Integer completionTokens
        ) {
        }
    }
}
