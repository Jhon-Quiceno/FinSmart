package com.smartfinance.backend.service.ai;

import com.smartfinance.backend.dto.analysis.AnalysisSummaryResponse;
import com.smartfinance.backend.dto.analysis.RecentTransactionResponse;
import com.smartfinance.backend.dto.analysis.TopCategoryResponse;
import com.smartfinance.backend.model.Debt;
import com.smartfinance.backend.model.RecurringPayment;
import com.smartfinance.backend.repository.DebtRepository;
import com.smartfinance.backend.repository.RecurringPaymentRepository;
import com.smartfinance.backend.service.FinancialAnalysisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialContextBuilderTest {

    @Mock
    private FinancialAnalysisService financialAnalysisService;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private RecurringPaymentRepository recurringPaymentRepository;

    @InjectMocks
    private FinancialContextBuilder contextBuilder;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildSystemPromptShouldIncludeSummaryTopCategoriesTransactionsDebtsAndRecurringPayments() {
        setAuthenticatedUser(1L);
        AnalysisSummaryResponse summary = new AnalysisSummaryResponse(
                2026, 7,
                BigDecimal.valueOf(5_000_000), BigDecimal.valueOf(3_000_000), BigDecimal.valueOf(2_000_000),
                new BigDecimal("0.6000"), false,
                BigDecimal.valueOf(1_000_000), new BigDecimal("0.2000"), BigDecimal.valueOf(24_000_000),
                List.of(new TopCategoryResponse(10L, "Comida", BigDecimal.valueOf(800_000), new BigDecimal("0.2667"))),
                List.of(),
                List.of(new RecentTransactionResponse(
                        1L, "EXPENSE", BigDecimal.valueOf(50_000), "Almuerzo", LocalDate.of(2026, 7, 1), "Comida"
                ))
        );
        when(financialAnalysisService.getSummary(null, null)).thenReturn(summary);

        Debt activeDebt = new Debt();
        activeDebt.setName("Tarjeta de crédito");
        activeDebt.setRemainingAmount(BigDecimal.valueOf(500_000));
        activeDebt.setDueDate(LocalDate.of(2026, 7, 15));
        Debt paidOffDebt = new Debt();
        paidOffDebt.setName("Préstamo pagado");
        paidOffDebt.setRemainingAmount(BigDecimal.ZERO);
        when(debtRepository.findAllByUser_Id(1L)).thenReturn(List.of(activeDebt, paidOffDebt));

        RecurringPayment payment = new RecurringPayment();
        payment.setName("Netflix");
        payment.setAmount(BigDecimal.valueOf(35_000));
        payment.setNextPaymentDate(LocalDate.of(2026, 7, 20));
        when(recurringPaymentRepository.findAllByUser_IdAndActiveTrue(1L)).thenReturn(List.of(payment));

        String prompt = contextBuilder.buildSystemPrompt();

        Assertions.assertTrue(prompt.contains("Responde siempre en español"));
        Assertions.assertTrue(prompt.contains(
                "Solo puedo ayudarte con preguntas sobre tu situación financiera registrada en FinSmart"));
        Assertions.assertTrue(prompt.contains("5.000.000"), prompt);
        Assertions.assertTrue(prompt.contains("Comida"));
        Assertions.assertTrue(prompt.contains("Almuerzo"));
        Assertions.assertTrue(prompt.contains("Tarjeta de crédito"));
        Assertions.assertTrue(prompt.contains("500.000"));
        Assertions.assertFalse(prompt.contains("Préstamo pagado"), "fully-paid debts must be excluded");
        Assertions.assertTrue(prompt.contains("Netflix"));
        Assertions.assertTrue(prompt.contains("35.000"));
        Assertions.assertTrue(prompt.contains("2026-07-20"));
    }

    @Test
    void buildSystemPromptShouldHandleEmptyDataGracefully() {
        setAuthenticatedUser(2L);
        AnalysisSummaryResponse summary = new AnalysisSummaryResponse(
                2026, 7, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), List.of()
        );
        when(financialAnalysisService.getSummary(null, null)).thenReturn(summary);
        when(debtRepository.findAllByUser_Id(2L)).thenReturn(List.of());
        when(recurringPaymentRepository.findAllByUser_IdAndActiveTrue(2L)).thenReturn(List.of());

        String prompt = contextBuilder.buildSystemPrompt();

        Assertions.assertTrue(prompt.contains("no tiene deudas activas"));
        Assertions.assertTrue(prompt.contains("no tiene servicios recurrentes activos"));
        Assertions.assertTrue(prompt.contains("No hay movimientos recientes"));
        Assertions.assertTrue(prompt.contains("No hay datos de categorías"));
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
