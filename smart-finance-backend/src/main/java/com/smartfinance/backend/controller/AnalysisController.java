package com.smartfinance.backend.controller;

import com.smartfinance.backend.dto.analysis.AnalysisSummaryResponse;
import com.smartfinance.backend.dto.analysis.RecommendationResponse;
import com.smartfinance.backend.service.FinancialAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for the current user's financial analysis, backed by
 * {@link FinancialAnalysisService}.
 *
 * <p>{@code year}/{@code month} are optional on both endpoints and default to the current
 * calendar year/month when omitted.
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final FinancialAnalysisService financialAnalysisService;

    public AnalysisController(FinancialAnalysisService financialAnalysisService) {
        this.financialAnalysisService = financialAnalysisService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalysisSummaryResponse> getSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(financialAnalysisService.getSummary(year, month));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(financialAnalysisService.getRecommendations(year, month));
    }
}
