package com.smartfinance.backend.ia.service.ai;

/**
 * A single turn sent to {@link AiChatClient}, in the OpenAI-compatible {@code {role, content}}
 * shape shared by every provider supported by this project (see
 * {@code docs/sprints/sprint5.md}, architecture decision 2).
 *
 * <p>This is distinct from {@link com.smartfinance.backend.ia.model.entity.AiMessageRole}: that enum is
 * the persisted, uppercase ({@code USER}/{@code ASSISTANT}) representation used in the
 * {@code ai_messages} table, while {@link #role} here is the lowercase string the provider's
 * API expects ({@code "system"}, {@code "user"}, {@code "assistant"}).
 *
 * @param role    one of {@link #ROLE_SYSTEM}, {@link #ROLE_USER}, {@link #ROLE_ASSISTANT}
 * @param content the message text
 */
public record ChatMessage(String role, String content) {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public static ChatMessage system(String content) {
        return new ChatMessage(ROLE_SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ROLE_USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ROLE_ASSISTANT, content);
    }
}
