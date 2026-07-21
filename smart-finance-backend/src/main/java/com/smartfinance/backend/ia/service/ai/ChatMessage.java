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
 * @param role     one of {@link #ROLE_SYSTEM}, {@link #ROLE_USER}, {@link #ROLE_ASSISTANT}
 * @param content  the message text
 * @param imageUrl {@code null} for a plain-text message; otherwise an image URL (an
 *                 {@code https://} URL or a {@code data:image/...;base64,...} data URI) sent
 *                 alongside {@code content} as a vision turn (see {@link #userWithImage}). Only
 *                 {@link AiChatOrchestrator#completeVision} is expected to receive messages
 *                 carrying this field — every other AI feature in this project only ever builds
 *                 plain-text messages through {@link #system}, {@link #user} or {@link #assistant}.
 */
public record ChatMessage(String role, String content, String imageUrl) {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public static ChatMessage system(String content) {
        return new ChatMessage(ROLE_SYSTEM, content, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ROLE_USER, content, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ROLE_ASSISTANT, content, null);
    }

    /**
     * Builds a user turn carrying both text and an image, for {@link AiChatOrchestrator#completeVision}.
     *
     * @param text     the instruction/question that accompanies the image (never {@code null})
     * @param imageUrl an {@code https://} URL or a {@code data:image/...;base64,...} data URI —
     *                 both are accepted as-is by OpenAI-compatible {@code image_url.url} fields,
     *                 so no image hosting step is required for a base64 photo
     * @return a user message carrying {@code imageUrl}, serialized by {@link AiChatClient} as the
     *         OpenAI vision array-of-parts content shape instead of a plain string
     */
    public static ChatMessage userWithImage(String text, String imageUrl) {
        return new ChatMessage(ROLE_USER, text, imageUrl);
    }
}
