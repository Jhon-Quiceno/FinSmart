package com.smartfinance.backend.dto.analysis;

/**
 * Relative urgency of a {@link RecommendationResponse} entry, so the dashboard can sort/style
 * alerts and recommendations without parsing {@link RecommendationResponse#message()}.
 */
public enum Severity {
    HIGH,
    MEDIUM,
    LOW
}
