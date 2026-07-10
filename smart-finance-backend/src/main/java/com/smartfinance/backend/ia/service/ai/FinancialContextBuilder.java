package com.smartfinance.backend.ia.service.ai;

import com.smartfinance.backend.analisis.model.dto.AnalysisSummaryResponse;
import com.smartfinance.backend.analisis.model.dto.RecentTransactionResponse;
import com.smartfinance.backend.analisis.model.dto.TopCategoryResponse;
import com.smartfinance.backend.deudas.model.entity.Debt;
import com.smartfinance.backend.servicios.model.entity.RecurringPayment;
import com.smartfinance.backend.deudas.repository.DebtRepository;
import com.smartfinance.backend.servicios.repository.RecurringPaymentRepository;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.analisis.service.FinancialAnalysisService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Builds the Spanish system prompt injected into every AI call for the current user — this is
 * FinSmart's "training": no fine-tuning happens, the model is simply given a compact snapshot of
 * the user's real financial data on every request (see {@code docs/sprints/sprint5.md},
 * architecture decision 4).
 *
 * <p>The prompt is built from {@link FinancialAnalysisService#getSummary}'s current-period
 * snapshot (income/expense/savings/ratios, top categories, and the up-to-10 recent transactions
 * it already returns) plus every active debt and active recurring payment, so a single call
 * assembles the whole context without duplicating the financial engine's aggregation logic here.
 */
@Component
public class FinancialContextBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int TOP_CATEGORIES_LIMIT = 3;
    private static final String INCOME_TYPE = "INCOME";

    private final FinancialAnalysisService financialAnalysisService;
    private final DebtRepository debtRepository;
    private final RecurringPaymentRepository recurringPaymentRepository;

    public FinancialContextBuilder(
            FinancialAnalysisService financialAnalysisService,
            DebtRepository debtRepository,
            RecurringPaymentRepository recurringPaymentRepository
    ) {
        this.financialAnalysisService = financialAnalysisService;
        this.debtRepository = debtRepository;
        this.recurringPaymentRepository = recurringPaymentRepository;
    }

    /**
     * Builds the full system prompt for the currently authenticated user (see
     * {@link SecurityUtils#getCurrentUserId()}), covering the current period.
     *
     * <p>Not {@code readOnly}: it delegates to {@link FinancialAnalysisService#getSummary}, which
     * also upserts the analysis snapshot as a side effect, so this transaction must allow writes.
     *
     * @return the assembled Spanish system prompt, ready to send as the first message to
     *         {@link AiChatClient}
     */
    @Transactional
    public String buildSystemPrompt() {
        Long userId = SecurityUtils.getCurrentUserId();
        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(null, null);

        List<Debt> activeDebts = debtRepository.findAllByUser_Id(userId).stream()
                .filter(debt -> debt.getRemainingAmount() != null && debt.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        List<RecurringPayment> activePayments = recurringPaymentRepository.findAllByUser_IdAndActiveTrue(userId);

        StringBuilder prompt = new StringBuilder();
        appendRulesBlock(prompt);
        appendSummaryBlock(prompt, summary);
        appendTopCategoriesBlock(prompt, summary.topCategories());
        appendRecentTransactionsBlock(prompt, summary.recentTransactions());
        appendDebtsBlock(prompt, activeDebts);
        appendRecurringPaymentsBlock(prompt, activePayments);
        return prompt.toString();
    }

    private void appendRulesBlock(StringBuilder prompt) {
        prompt.append("Eres el asistente financiero personal de FinSmart para el usuario autenticado. Reglas:\n")
                .append("- Responde siempre en español.\n")
                .append("- Utiliza únicamente los datos financieros provistos a continuación; si falta un dato, "
                        + "indícalo explícitamente en lugar de inventarlo.\n")
                .append("- Expresa los montos en pesos colombianos (COP) con separador de miles "
                        + "(ejemplo: $1.234.567).\n")
                .append("- Tus respuestas son orientativas y no constituyen asesoría financiera regulada.\n")
                .append("- Responde únicamente preguntas relacionadas con la situación financiera personal del "
                        + "usuario (ingresos, gastos, deudas, pagos recurrentes, ahorros, categorías o "
                        + "recomendaciones financieras) utilizando los datos provistos en este mensaje.\n")
                .append("- Si te preguntan sobre cualquier otro tema (deportes, política, entretenimiento, "
                        + "programación, cultura general o cualquier asunto no financiero), responde "
                        + "exclusivamente con este mensaje, sin agregar información adicional: "
                        + "\"Solo puedo ayudarte con preguntas sobre tu situación financiera registrada en "
                        + "FinSmart. ¿Querés que hablemos de tus gastos, ingresos, deudas o ahorros?\"\n\n");
    }

    private void appendSummaryBlock(StringBuilder prompt, AnalysisSummaryResponse summary) {
        prompt.append(String.format(Locale.ROOT, "Resumen financiero del periodo %02d/%d:%n",
                        summary.periodMonth(), summary.periodYear()))
                .append("- Ingresos: $").append(formatCop(summary.totalIncome())).append('\n')
                .append("- Gastos: $").append(formatCop(summary.totalExpense())).append('\n')
                .append("- Balance (ahorro): $").append(formatCop(summary.savings())).append('\n')
                .append("- Porcentaje de gasto sobre ingreso: ").append(formatPercentage(summary.expenseRatio())).append('\n')
                .append("- Porcentaje de deuda sobre ingreso: ").append(formatPercentage(summary.debtRatio())).append("\n\n");
    }

    private void appendTopCategoriesBlock(StringBuilder prompt, List<TopCategoryResponse> topCategories) {
        prompt.append("Categorías con mayor gasto este periodo:\n");
        if (topCategories.isEmpty()) {
            prompt.append("- No hay datos de categorías para este periodo.\n\n");
            return;
        }
        topCategories.stream()
                .limit(TOP_CATEGORIES_LIMIT)
                .forEach(category -> prompt.append("- ").append(category.categoryName())
                        .append(": $").append(formatCop(category.totalAmount())).append('\n'));
        prompt.append('\n');
    }

    private void appendRecentTransactionsBlock(StringBuilder prompt, List<RecentTransactionResponse> transactions) {
        prompt.append("Movimientos recientes:\n");
        if (transactions.isEmpty()) {
            prompt.append("- No hay movimientos recientes registrados.\n\n");
            return;
        }
        transactions.forEach(transaction -> {
            String typeLabel = INCOME_TYPE.equals(transaction.type()) ? "Ingreso" : "Gasto";
            String category = transaction.categoryName() != null ? transaction.categoryName() : "Sin categoría";
            String description = transaction.description() != null && !transaction.description().isBlank()
                    ? " | " + transaction.description()
                    : "";
            prompt.append("- ").append(transaction.date().format(DATE_FORMAT))
                    .append(" | ").append(typeLabel)
                    .append(" | $").append(formatCop(transaction.amount()))
                    .append(" | ").append(category)
                    .append(description)
                    .append('\n');
        });
        prompt.append('\n');
    }

    private void appendDebtsBlock(StringBuilder prompt, List<Debt> activeDebts) {
        prompt.append("Deudas activas:\n");
        if (activeDebts.isEmpty()) {
            prompt.append("- El usuario no tiene deudas activas registradas.\n\n");
            return;
        }
        activeDebts.forEach(debt -> {
            String dueDate = debt.getDueDate() != null ? ", vence el " + debt.getDueDate().format(DATE_FORMAT) : "";
            prompt.append("- ").append(debt.getName())
                    .append(": saldo restante $").append(formatCop(debt.getRemainingAmount()))
                    .append(dueDate)
                    .append('\n');
        });
        prompt.append('\n');
    }

    private void appendRecurringPaymentsBlock(StringBuilder prompt, List<RecurringPayment> activePayments) {
        prompt.append("Servicios recurrentes activos:\n");
        if (activePayments.isEmpty()) {
            prompt.append("- El usuario no tiene servicios recurrentes activos registrados.\n");
            return;
        }
        activePayments.forEach(payment -> prompt.append("- ").append(payment.getName())
                .append(": $").append(formatCop(payment.getAmount()))
                .append(", próximo pago ").append(payment.getNextPaymentDate().format(DATE_FORMAT))
                .append('\n'));
    }

    /**
     * Formats {@code amount} as a whole-COP-peso figure with {@code .} thousands separators
     * (e.g. {@code 1234567} -> {@code "1.234.567"}), using an explicit {@link DecimalFormatSymbols}
     * instead of a {@code es-CO} {@link Locale} so the output does not depend on the JVM's
     * bundled locale data being available.
     */
    private static String formatCop(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal rounded = safeAmount.setScale(0, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(rounded);
    }

    private static String formatPercentage(BigDecimal ratio) {
        BigDecimal safeRatio = ratio != null ? ratio : BigDecimal.ZERO;
        BigDecimal percentage = safeRatio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        return percentage + "%";
    }
}
