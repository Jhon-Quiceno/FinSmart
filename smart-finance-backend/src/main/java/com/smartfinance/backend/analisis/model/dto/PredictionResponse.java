package com.smartfinance.backend.analisis.model.dto;

import java.math.BigDecimal;

/**
 * Month-end financial projection for the current user, returned by
 * {@code GET /api/analysis/prediction} and computed by
 * {@code com.smartfinance.backend.analisis.service.MonthEndPredictionService}.
 *
 * @param projectedExpense    expected total expense by the end of the current month
 *                             ({@code spentSoFar + avgDailySpend * daysRemaining})
 * @param projectedBalance    {@code monthIncome - projectedExpense}; negative means the user is
 *                             on track to end the month over budget
 * @param avgDailySpend       average daily expense so far this month
 *                             ({@code spentSoFar / daysElapsed})
 * @param daysRemaining       true days left in the month ({@code 0} on the last day) — display
 *                             only, not used as a division denominator by the client
 * @param recommendedDailyMax remaining budget ({@code monthIncome - spentSoFar}) divided evenly
 *                             across the days left in the month (clamped to at least {@code 1}
 *                             internally so this never divides by zero), floored at {@code 0}
 *                             when the remaining budget is already negative or zero
 * @param alert                {@code true} when {@code projectedBalance} is negative
 */
public record PredictionResponse(
        BigDecimal projectedExpense,
        BigDecimal projectedBalance,
        BigDecimal avgDailySpend,
        int daysRemaining,
        BigDecimal recommendedDailyMax,
        boolean alert
) {
}
