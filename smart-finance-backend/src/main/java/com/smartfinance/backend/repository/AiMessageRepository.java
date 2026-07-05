package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.AiMessage;
import com.smartfinance.backend.model.AiMessageKind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for {@link AiMessage}, always scoped by owner.
 *
 * <p>Consumed starting in Batch 2 of Sprint 5 (chat history and dashboard insights); created
 * now alongside the {@code ai_messages} table so the schema and entity land together.
 */
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    Page<AiMessage> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Used by {@code AiChatService} both for {@code GET /api/ai/chat/history} and to load the
     * recent-turns window sent to the AI provider for conversation continuity — {@link AiMessageKind#INSIGHT}
     * rows must never leak into either, hence the kind filter (unlike
     * {@link #findByUser_IdOrderByCreatedAtDesc}, which predates this filter and is unused by
     * chat/insight code).
     */
    Page<AiMessage> findByUser_IdAndKindOrderByCreatedAtDesc(Long userId, AiMessageKind kind, Pageable pageable);

    Optional<AiMessage> findFirstByUser_IdAndKindOrderByCreatedAtDesc(Long userId, AiMessageKind kind);
}
