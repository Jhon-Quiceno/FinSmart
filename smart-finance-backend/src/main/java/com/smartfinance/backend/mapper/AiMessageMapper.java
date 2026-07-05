package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.ai.ChatMessageResponse;
import com.smartfinance.backend.dto.ai.InsightResponse;
import com.smartfinance.backend.model.AiMessage;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper between {@link AiMessage} and its read DTOs.
 *
 * <p>{@link AiMessage} is never created from a client-supplied DTO (chat turns and insights are
 * always built server-side by {@code AiChatService}/{@code AiInsightService}), so this mapper
 * only needs the two read directions used by {@code GET /api/ai/chat/history} and
 * {@code GET /api/ai/insights} respectively.
 */
@Mapper(componentModel = "spring")
public interface AiMessageMapper {

    ChatMessageResponse toChatResponse(AiMessage message);

    InsightResponse toInsightResponse(AiMessage message);
}
