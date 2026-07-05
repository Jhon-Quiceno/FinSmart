package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.analysis.PredictionResponse;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.repository.IncomeRepository;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Computes the month-end spending projection for a user, backing
 * {@code GET /api/analysis/prediction} (Sprint 5, Batch 3) and the daily alert dispatched by
 * {@code MonthEndPredictionJob} when the projection turns negative.
 *
 * <p>Deliberately does NOT reuse {@link FinancialAnalysisService#getSummary}: that method
 * resolves the caller via {@link SecurityUtils#getCurrentUserId()} (unusable from a job with no
 * security context) and, as a side effect, upserts an analysis snapshot row — a side effect this
 * class must not trigger every time a job checks a projection. Instead it queries
 * {@link IncomeRepository}/{@link ExpenseRepository} directly with an explicit {@code userId},
 * which those repositories already support.
 */
@Service
public class MonthEndPredictionService {

    private static final int MONEY_SCALE = 2;

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    public MonthEndPredictionService(IncomeRepository incomeRepository, ExpenseRepository expenseRepository) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Projects {@code userId}'s end-of-month financial position as of {@code today}.
     *
     * <p>{@code trueDaysRemaining} (the literal day count left in the month, {@code 0} on the
     * last day) drives {@code projectedExpense} and the returned {@code daysRemaining}: using the
     * clamped-to-1 value there would add a full phantom day of projected spend on the last day of
     * the month, which can push {@code projectedBalance} negative and trigger a false overspend
     * alert when the user has, in fact, already finished the month. The clamp to at least
     * {@code 1} is applied only to the {@code recommendedDailyMax} divisor, where a {@code 0}
     * would be a division by zero; on day one of the month, {@code avgDailySpend} equals that
     * day's spend, since {@code daysElapsed} is {@code 1}.
     *
     * @param userId owner of the incomes/expenses to project
     * @param today  the date to project from — the caller's "now", injectable for testability
     * @return the computed projection
     */
    @Transactional(readOnly = true)
    public PredictionResponse computePrediction(Long userId, LocalDate today) {
        YearMonth period = YearMonth.from(today);
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();

        int daysElapsed = today.getDayOfMonth();
        int trueDaysRemaining = period.lengthOfMonth() - daysElapsed;
        int recommendedDailyMaxDivisor = Math.max(trueDaysRemaining, 1);

        BigDecimal spentSoFar = nullSafe(expenseRepository.sumAmountByUserAndPeriod(userId, periodStart, today));
        BigDecimal monthIncome = nullSafe(incomeRepository.sumAmountByUserAndPeriod(userId, periodStart, periodEnd));

        BigDecimal avgDailySpend = spentSoFar.divide(BigDecimal.valueOf(daysElapsed), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal projectedExpense = spentSoFar.add(avgDailySpend.multiply(BigDecimal.valueOf(trueDaysRemaining)));
        BigDecimal projectedBalance = monthIncome.subtract(projectedExpense);

        BigDecimal remainingBudget = monthIncome.subtract(spentSoFar);
        BigDecimal recommendedDailyMax = remainingBudget.compareTo(BigDecimal.ZERO) > 0
                ? remainingBudget.divide(BigDecimal.valueOf(recommendedDailyMaxDivisor), MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(MONEY_SCALE);

        boolean alert = projectedBalance.compareTo(BigDecimal.ZERO) < 0;

        return new PredictionResponse(projectedExpense, projectedBalance, avgDailySpend, trueDaysRemaining, recommendedDailyMax, alert);
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
