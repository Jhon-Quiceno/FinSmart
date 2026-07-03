package com.smartfinance.backend.dto.analysis;

/**
 * Classifies a {@link RecommendationResponse} entry produced by the recommendation engine
 * ({@code FinancialAnalysisService#getRecommendations}).
 */
public enum RecommendationType {
    ALERT,
    RECOMMENDATION,
    INFO
}
