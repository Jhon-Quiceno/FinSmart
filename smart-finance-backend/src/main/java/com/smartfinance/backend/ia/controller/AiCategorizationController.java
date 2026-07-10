package com.smartfinance.backend.ia.controller;

import com.smartfinance.backend.ia.model.dto.CategorizeRequest;
import com.smartfinance.backend.ia.model.dto.CategorizeResponse;
import com.smartfinance.backend.ia.service.AiCategorizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for AI-assisted expense category suggestion.
 */
@RestController
@RequestMapping("/api/ai/categorize")
public class AiCategorizationController {

    private final AiCategorizationService service;

    public AiCategorizationController(AiCategorizationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CategorizeResponse> categorize(@Valid @RequestBody CategorizeRequest request) {
        return ResponseEntity.ok(service.categorize(request));
    }
}
