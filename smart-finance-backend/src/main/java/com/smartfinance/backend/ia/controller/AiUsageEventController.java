package com.smartfinance.backend.ia.controller;

import com.smartfinance.backend.ia.model.dto.AiUsageEventSummaryResponse;
import com.smartfinance.backend.ia.service.AiUsageEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * REST endpoint for the granular AI usage tracking ({@code ai_usage_events}):
 * {@code GET /api/ai/usage} reports token consumption across every tracked AI operation (chat,
 * categorize, insight) for a given UTC calendar month, defaulting to the current month when
 * {@code period} is omitted.
 *
 * <p>Distinct from {@code GET /api/ai/chat/usage}, which reports the monthly chat-message quota
 * ({@code V14}) — this endpoint closes item 9 of {@code docs/sprints/sprint1.md} ("expone un
 * resumen de uso por usuario/periodo").
 */
@RestController
@RequestMapping("/api/ai/usage")
public class AiUsageEventController {

    private final AiUsageEventService aiUsageEventService;

    public AiUsageEventController(AiUsageEventService aiUsageEventService) {
        this.aiUsageEventService = aiUsageEventService;
    }

    @GetMapping
    public ResponseEntity<AiUsageEventSummaryResponse> getUsageSummary(
            @RequestParam(required = false) String period
    ) {
        YearMonth resolvedPeriod = parsePeriod(period);
        return ResponseEntity.ok(aiUsageEventService.getUsageSummary(resolvedPeriod));
    }

    private static YearMonth parsePeriod(String period) {
        if (period == null || period.isBlank()) {
            return YearMonth.now(ZoneOffset.UTC);
        }
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("El periodo debe tener el formato yyyy-MM");
        }
    }
}
