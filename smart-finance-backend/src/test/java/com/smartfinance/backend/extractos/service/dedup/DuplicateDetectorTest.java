package com.smartfinance.backend.extractos.service.dedup;

import com.smartfinance.backend.extractos.model.MovementType;
import com.smartfinance.backend.extractos.model.ParsedTransaction;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.ingresos.model.entity.Income;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class DuplicateDetectorTest {

    private final DuplicateDetector duplicateDetector = new DuplicateDetector();

    @Test
    void detectDuplicatesShouldFlagExpenseMatchingAmountDateAndDescription() {
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 10), "Compra supermercado", BigDecimal.valueOf(100), MovementType.EXPENSE, null, null
        );
        Expense existingExpense = buildExpense(BigDecimal.valueOf(100), LocalDate.of(2026, 6, 10), "Compra supermercado");

        List<Boolean> result = duplicateDetector.detectDuplicates(
                List.of(transaction), List.of(existingExpense), List.of()
        );

        Assertions.assertEquals(List.of(true), result);
    }

    @Test
    void detectDuplicatesShouldFlagWhenDateDifferenceIsExactlyThreeDays() {
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 10), "Compra supermercado", BigDecimal.valueOf(100), MovementType.EXPENSE, null, null
        );
        Expense existingExpense = buildExpense(BigDecimal.valueOf(100), LocalDate.of(2026, 6, 13), "Compra supermercado");

        List<Boolean> result = duplicateDetector.detectDuplicates(
                List.of(transaction), List.of(existingExpense), List.of()
        );

        Assertions.assertEquals(List.of(true), result);
    }

    @Test
    void detectDuplicatesShouldNotFlagWhenDateDifferenceExceedsThreeDays() {
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 10), "Compra supermercado", BigDecimal.valueOf(100), MovementType.EXPENSE, null, null
        );
        Expense existingExpense = buildExpense(BigDecimal.valueOf(100), LocalDate.of(2026, 6, 14), "Compra supermercado");

        List<Boolean> result = duplicateDetector.detectDuplicates(
                List.of(transaction), List.of(existingExpense), List.of()
        );

        Assertions.assertEquals(List.of(false), result);
    }

    @Test
    void detectDuplicatesShouldNotFlagWhenAmountDiffers() {
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 10), "Compra supermercado", BigDecimal.valueOf(100), MovementType.EXPENSE, null, null
        );
        Expense existingExpense = buildExpense(BigDecimal.valueOf(101), LocalDate.of(2026, 6, 10), "Compra supermercado");

        List<Boolean> result = duplicateDetector.detectDuplicates(
                List.of(transaction), List.of(existingExpense), List.of()
        );

        Assertions.assertEquals(List.of(false), result);
    }

    @Test
    void detectDuplicatesShouldNotFlagWhenDescriptionsAreNotSimilar() {
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 10), "Compra supermercado", BigDecimal.valueOf(100), MovementType.EXPENSE, null, null
        );
        Expense existingExpense = buildExpense(BigDecimal.valueOf(100), LocalDate.of(2026, 6, 10), "Pago servicio de luz");

        List<Boolean> result = duplicateDetector.detectDuplicates(
                List.of(transaction), List.of(existingExpense), List.of()
        );

        Assertions.assertEquals(List.of(false), result);
    }

    @Test
    void detectDuplicatesShouldOnlyCompareExpenseRowsAgainstExpenses() {
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 10), "Compra supermercado", BigDecimal.valueOf(100), MovementType.EXPENSE, null, null
        );
        Income matchingIncome = buildIncome(BigDecimal.valueOf(100), LocalDate.of(2026, 6, 10), "Compra supermercado");

        List<Boolean> result = duplicateDetector.detectDuplicates(
                List.of(transaction), List.of(), List.of(matchingIncome)
        );

        Assertions.assertEquals(List.of(false), result);
    }

    @Test
    void detectDuplicatesShouldOnlyCompareIncomeRowsAgainstIncomes() {
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 10), "Pago nomina", BigDecimal.valueOf(2000), MovementType.INCOME, null, null
        );
        Expense matchingExpense = buildExpense(BigDecimal.valueOf(2000), LocalDate.of(2026, 6, 10), "Pago nomina");

        List<Boolean> result = duplicateDetector.detectDuplicates(
                List.of(transaction), List.of(matchingExpense), List.of()
        );

        Assertions.assertEquals(List.of(false), result);
    }

    @Test
    void detectDuplicatesShouldFlagIncomeMatchingAnExistingIncome() {
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 10), "Pago nomina", BigDecimal.valueOf(2000), MovementType.INCOME, null, null
        );
        Income existingIncome = buildIncome(BigDecimal.valueOf(2000), LocalDate.of(2026, 6, 9), "Pago nomina");

        List<Boolean> result = duplicateDetector.detectDuplicates(
                List.of(transaction), List.of(), List.of(existingIncome)
        );

        Assertions.assertEquals(List.of(true), result);
    }

    private static Expense buildExpense(BigDecimal amount, LocalDate date, String description) {
        Expense expense = new Expense();
        expense.setAmount(amount);
        expense.setDate(date);
        expense.setDescription(description);
        expense.setPaymentMethod(PaymentMethodType.OTHER);
        return expense;
    }

    private static Income buildIncome(BigDecimal amount, LocalDate date, String description) {
        Income income = new Income();
        income.setAmount(amount);
        income.setDate(date);
        income.setDescription(description);
        return income;
    }
}
