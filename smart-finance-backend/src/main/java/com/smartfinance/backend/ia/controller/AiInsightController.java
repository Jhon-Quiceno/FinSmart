package com.smartfinance.backend.ia.controller;

import com.smartfinance.backend.ia.model.dto.InsightResponse;
import com.smartfinance.backend.ia.service.AiInsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for AI-generated financial insights: {@code GET /api/ai/insights} returns the
 * current user's latest generated insight (if any), {@code POST /api/ai/insights/generate}
 * generates and persists a new one.
 */
@RestController
@RequestMapping("/api/ai/insights")
public class AiInsightController {

    private final AiInsightService aiInsightService;

    public AiInsightController(AiInsightService aiInsightService) {
        this.aiInsightService = aiInsightService;
    }

    @GetMapping
    public ResponseEntity<InsightResponse> getLatest() {
        InsightResponse response = aiInsightService.getLatestInsight();
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<InsightResponse> generate() {
        return ResponseEntity.ok(aiInsightService.generateInsight());
    }
}
