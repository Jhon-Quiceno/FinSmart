package com.smartfinance.backend.extractos.service;

import com.smartfinance.backend.extractos.model.MovementType;
import com.smartfinance.backend.extractos.model.ParsedTransaction;
import com.smartfinance.backend.extractos.model.dto.ImportConfirmRow;
import com.smartfinance.backend.extractos.model.dto.StatementConfirmRequest;
import com.smartfinance.backend.extractos.model.dto.StatementImportResultResponse;
import com.smartfinance.backend.extractos.model.dto.StatementPreviewResponse;
import com.smartfinance.backend.extractos.service.ai.StatementAiExtractionService;
import com.smartfinance.backend.extractos.service.dedup.DuplicateDetector;
import com.smartfinance.backend.extractos.service.extraction.StatementTextExtractionService;
import com.smartfinance.backend.gastos.model.dto.ExpenseRequest;
import com.smartfinance.backend.gastos.model.dto.ExpenseResponse;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.CategoryRepository;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.gastos.service.ExpenseService;
import com.smartfinance.backend.ingresos.model.dto.IncomeRequest;
import com.smartfinance.backend.ingresos.model.dto.IncomeResponse;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.ingresos.service.IncomeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementImportServiceTest {

    @Mock
    private StatementTextExtractionService statementTextExtractionService;

    @Mock
    private StatementAiExtractionService statementAiExtractionService;

    @Mock
    private DuplicateDetector duplicateDetector;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private IncomeService incomeService;

    private StatementImportService statementImportService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        statementImportService = new StatementImportService(
                statementTextExtractionService, statementAiExtractionService, duplicateDetector,
                categoryRepository, expenseRepository, incomeRepository, expenseService, incomeService
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void previewShouldNotPersistAnything() {
        setAuthenticatedUser(1L);
        MockMultipartFile file = new MockMultipartFile("file", "extracto.csv", "text/csv", "contenido".getBytes());
        when(statementTextExtractionService.extractText(eq("extracto.csv"), any(byte[].class), eq(null)))
                .thenReturn("contenido del extracto");
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(1L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(1L, CategoryType.EXPENSE)).thenReturn(List.of());
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 1), "Compra", BigDecimal.valueOf(100), MovementType.EXPENSE, null, null
        );
        when(statementAiExtractionService.extract("contenido del extracto", List.of(), List.of(), 1L))
                .thenReturn(List.of(transaction));
        when(expenseRepository.findByUserAndPeriod(eq(1L), any(), any())).thenReturn(List.of());
        when(incomeRepository.findByUserAndPeriod(eq(1L), any(), any())).thenReturn(List.of());
        when(duplicateDetector.detectDuplicates(anyList(), anyList(), anyList())).thenReturn(List.of(false));

        StatementPreviewResponse response = statementImportService.preview(file, null);

        Assertions.assertEquals(1, response.totalRows());
        Assertions.assertEquals(0, response.duplicateRows());
        verifyNoInteractions(expenseService, incomeService);
        verify(expenseRepository, never()).save(any());
        verify(incomeRepository, never()).save(any());
    }

    @Test
    void previewShouldMarkDuplicateRowsFromDetector() {
        setAuthenticatedUser(2L);
        MockMultipartFile file = new MockMultipartFile("file", "extracto.csv", "text/csv", "contenido".getBytes());
        when(statementTextExtractionService.extractText(eq("extracto.csv"), any(byte[].class), eq(null)))
                .thenReturn("texto");
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(2L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(2L, CategoryType.EXPENSE)).thenReturn(List.of());
        ParsedTransaction transaction = new ParsedTransaction(
                LocalDate.of(2026, 6, 1), "Compra", BigDecimal.valueOf(100), MovementType.EXPENSE, null, null
        );
        when(statementAiExtractionService.extract("texto", List.of(), List.of(), 2L)).thenReturn(List.of(transaction));
        when(expenseRepository.findByUserAndPeriod(eq(2L), any(), any())).thenReturn(List.of());
        when(incomeRepository.findByUserAndPeriod(eq(2L), any(), any())).thenReturn(List.of());
        when(duplicateDetector.detectDuplicates(anyList(), anyList(), anyList())).thenReturn(List.of(true));

        StatementPreviewResponse response = statementImportService.preview(file, null);

        Assertions.assertEquals(1, response.duplicateRows());
        Assertions.assertTrue(response.rows().get(0).isDuplicate());
    }

    @Test
    void confirmShouldCreateOnlyTheGivenRows() {
        setAuthenticatedUser(3L);
        ImportConfirmRow expenseRow = new ImportConfirmRow(
                MovementType.EXPENSE, BigDecimal.valueOf(50), LocalDate.of(2026, 6, 1), "Compra", null, PaymentMethodType.CASH
        );
        when(expenseService.createExpense(eq(3L), any(ExpenseRequest.class))).thenReturn(
                new ExpenseResponse(1L, BigDecimal.valueOf(50), "Compra", LocalDate.of(2026, 6, 1), PaymentMethodType.CASH, null, null, null)
        );

        StatementImportResultResponse result = statementImportService.confirm(new StatementConfirmRequest(List.of(expenseRow)));

        Assertions.assertEquals(1, result.createdCount());
        verify(expenseService, times(1)).createExpense(eq(3L), any(ExpenseRequest.class));
        verifyNoInteractions(incomeService);
    }

    @Test
    void confirmShouldDefaultPaymentMethodToOtherWhenMissing() {
        setAuthenticatedUser(4L);
        ImportConfirmRow expenseRow = new ImportConfirmRow(
                MovementType.EXPENSE, BigDecimal.valueOf(50), LocalDate.of(2026, 6, 1), "Compra", null, null
        );
        when(expenseService.createExpense(eq(4L), any(ExpenseRequest.class))).thenReturn(
                new ExpenseResponse(1L, BigDecimal.valueOf(50), "Compra", LocalDate.of(2026, 6, 1), PaymentMethodType.OTHER, null, null, null)
        );

        statementImportService.confirm(new StatementConfirmRequest(List.of(expenseRow)));

        ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
        verify(expenseService).createExpense(eq(4L), captor.capture());
        Assertions.assertEquals(PaymentMethodType.OTHER, captor.getValue().paymentMethod());
    }

    @Test
    void confirmShouldRouteIncomeRowsToIncomeService() {
        setAuthenticatedUser(5L);
        ImportConfirmRow incomeRow = new ImportConfirmRow(
                MovementType.INCOME, BigDecimal.valueOf(2000), LocalDate.of(2026, 6, 1), "Salario", null, null
        );
        when(incomeService.createIncome(eq(5L), any(IncomeRequest.class))).thenReturn(
                new IncomeResponse(1L, BigDecimal.valueOf(2000), "Salario", LocalDate.of(2026, 6, 1), null, null)
        );

        StatementImportResultResponse result = statementImportService.confirm(new StatementConfirmRequest(List.of(incomeRow)));

        Assertions.assertEquals(1, result.createdCount());
        verify(incomeService, times(1)).createIncome(eq(5L), any(IncomeRequest.class));
        verifyNoInteractions(expenseService);
    }

    @Test
    void confirmShouldCreateOneMovementPerRowAndReturnTotalCount() {
        setAuthenticatedUser(6L);
        ImportConfirmRow expenseRow = new ImportConfirmRow(
                MovementType.EXPENSE, BigDecimal.valueOf(50), LocalDate.of(2026, 6, 1), "Compra", null, PaymentMethodType.CASH
        );
        ImportConfirmRow incomeRow = new ImportConfirmRow(
                MovementType.INCOME, BigDecimal.valueOf(2000), LocalDate.of(2026, 6, 2), "Salario", null, null
        );
        when(expenseService.createExpense(eq(6L), any(ExpenseRequest.class))).thenReturn(
                new ExpenseResponse(1L, BigDecimal.valueOf(50), "Compra", LocalDate.of(2026, 6, 1), PaymentMethodType.CASH, null, null, null)
        );
        when(incomeService.createIncome(eq(6L), any(IncomeRequest.class))).thenReturn(
                new IncomeResponse(2L, BigDecimal.valueOf(2000), "Salario", LocalDate.of(2026, 6, 2), null, null)
        );

        StatementImportResultResponse result = statementImportService.confirm(
                new StatementConfirmRequest(List.of(expenseRow, incomeRow))
        );

        Assertions.assertEquals(2, result.createdCount());
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
