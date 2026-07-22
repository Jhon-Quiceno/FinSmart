package com.smartfinance.backend.ia.model.dto;

import com.smartfinance.backend.gastos.model.entity.CategoryType;

/**
 * Result of {@code FinancialSummaryQueryService#parseQuery}: what a free-text financial-summary
 * question (e.g. {@code "¿Cuánto gasté en comida este mes?"}) is asking for.
 *
 * @param period       the date range the question refers to; defaults to {@link SummaryPeriod#MONTH}
 *                      when no period is stated explicitly
 * @param movementType {@link CategoryType#EXPENSE} for a spending question, {@link CategoryType#INCOME}
 *                      for an earning question, or {@code null} for a balance/general question that
 *                      covers both
 * @param categoryName free-text name of the category mentioned in the question (e.g. {@code "comida"}),
 *                      or {@code null} when no specific category was named
 * @param topic        whether the question is about debts or about movements; defaults to
 *                      {@link SummaryTopic#MOVEMENT} when the topic could not be decided. When
 *                      {@code topic} is {@link SummaryTopic#DEBT}, {@code period}/{@code movementType}/
 *                      {@code categoryName} are irrelevant — a debt query answers with the current
 *                      outstanding balance, not a movement over a date range.
 */
public record SummaryQueryIntent(
        SummaryPeriod period,
        CategoryType movementType,
        String categoryName,
        SummaryTopic topic
) {
}
