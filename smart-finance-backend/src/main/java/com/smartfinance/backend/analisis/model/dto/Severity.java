package com.smartfinance.backend.analisis.model.dto;

/**
 * Relative urgency of a {@link RecommendationResponse} entry, so the dashboard can sort/style
 * alerts and recommendations without parsing {@link RecommendationResponse#message()}.
 */
public enum Severity {
    HIGH,
    MEDIUM,
    LOW
}
