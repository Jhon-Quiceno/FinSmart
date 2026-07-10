package com.smartfinance.backend.analisis.controller;

import com.smartfinance.backend.analisis.model.dto.AnalysisSummaryResponse;
import com.smartfinance.backend.analisis.model.dto.PredictionResponse;
import com.smartfinance.backend.analisis.model.dto.RecommendationResponse;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.analisis.service.FinancialAnalysisService;
import com.smartfinance.backend.analisis.service.MonthEndPredictionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * REST endpoints for the current user's financial analysis, backed by
 * {@link FinancialAnalysisService} and {@link MonthEndPredictionService}.
 *
 * <p>{@code year}/{@code month} are optional on the summary/recommendations endpoints and
 * default to the current calendar year/month when omitted.
 */
@RestController
@RequestMapping("/api/analysis")
@Validated
public class AnalysisController {

    private final FinancialAnalysisService financialAnalysisService;
    private final MonthEndPredictionService monthEndPredictionService;
    private final Clock clock;

    public AnalysisController(
            FinancialAnalysisService financialAnalysisService,
            MonthEndPredictionService monthEndPredictionService,
            Clock clock
    ) {
        this.financialAnalysisService = financialAnalysisService;
        this.monthEndPredictionService = monthEndPredictionService;
        this.clock = clock;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalysisSummaryResponse> getSummary(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        return ResponseEntity.ok(financialAnalysisService.getSummary(year, month));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        return ResponseEntity.ok(financialAnalysisService.getRecommendations(year, month));
    }

    /**
     * Month-end spending projection for the current user, computed as of today (see
     * {@link MonthEndPredictionService#computePrediction}).
     */
    @GetMapping("/prediction")
    public ResponseEntity<PredictionResponse> getPrediction() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(monthEndPredictionService.computePrediction(userId, LocalDate.now(clock)));
    }
}
