package com.smartfinance.backend.ia.controller;

import com.smartfinance.backend.ia.model.dto.AiProviderStatusResponse;
import com.smartfinance.backend.ia.service.ai.AiProviderRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only status of the app-level AI providers configured via environment variables (see
 * {@code docs/sprints/sprint5.md}) — feeds the informational card in {@code /configuracion}.
 * Intentionally exposes no mutation endpoint: providers are configured by the app operator, not
 * end users.
 */
@RestController
@RequestMapping("/api/ai/providers")
public class AiProviderStatusController {

    private final AiProviderRegistry registry;

    public AiProviderStatusController(AiProviderRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/status")
    public ResponseEntity<List<AiProviderStatusResponse>> status() {
        List<AiProviderStatusResponse> response = registry.status().stream()
                .map(status -> new AiProviderStatusResponse(status.name(), status.configured(), status.priority()))
                .toList();
        return ResponseEntity.ok(response);
    }
}
