package com.smartfinance.backend.ia.service.ai;

import com.smartfinance.backend.ia.model.entity.AiUsageEventType;

/**
 * Attribution needed by {@link AiChatOrchestrator} to record telemetry for a {@code complete}/
 * {@code completeVision} call on the caller's behalf: who the call is for, and which AI-backed
 * operation it represents.
 *
 * <p>Passed to {@link AiChatOrchestrator#complete(java.util.List, AiCallContext)}/
 * {@link AiChatOrchestrator#completeVision(java.util.List, AiCallContext)} instead of having every
 * caller record its own {@code AiUsageEvent} after the fact — see the class Javadoc on
 * {@link AiChatOrchestrator} for why per-attempt telemetry (latency, success/failure, error class)
 * can only be captured from inside the failover loop itself, not by the caller.
 *
 * <p>{@code ctx == null} is a valid, supported value on both overloads: it means "don't record
 * telemetry for this call" and is exactly what the no-context overloads
 * ({@link AiChatOrchestrator#complete(java.util.List)}/{@link AiChatOrchestrator#completeVision(java.util.List)})
 * delegate to, preserving their existing no-telemetry contract for callers that don't pass one
 * (today, only {@code StatementAiExtractionService}, which is out of scope for this change and
 * keeps recording usage itself).
 *
 * @param userId    the user the AI call is made on behalf of
 * @param eventType which AI-backed operation this call represents (see
 *                  {@code AiUsageEventService#recordAttempt})
 */
public record AiCallContext(Long userId, AiUsageEventType eventType) {
}
