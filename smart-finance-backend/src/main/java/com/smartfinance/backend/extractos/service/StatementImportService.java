package com.smartfinance.backend.extractos.service;

import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.extractos.exception.StatementExtractionException;
import com.smartfinance.backend.extractos.exception.UnsupportedStatementFileException;
import com.smartfinance.backend.extractos.model.MovementType;
import com.smartfinance.backend.extractos.model.ParsedTransaction;
import com.smartfinance.backend.extractos.model.dto.ImportConfirmRow;
import com.smartfinance.backend.extractos.model.dto.ImportPreviewRow;
import com.smartfinance.backend.extractos.model.dto.StatementConfirmRequest;
import com.smartfinance.backend.extractos.model.dto.StatementImportResultResponse;
import com.smartfinance.backend.extractos.model.dto.StatementPreviewResponse;
import com.smartfinance.backend.extractos.service.ai.StatementAiExtractionService;
import com.smartfinance.backend.extractos.service.dedup.DuplicateDetector;
import com.smartfinance.backend.extractos.service.extraction.StatementTextExtractionService;
import com.smartfinance.backend.gastos.model.dto.ExpenseRequest;
import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.CategoryRepository;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.gastos.service.ExpenseService;
import com.smartfinance.backend.ingresos.model.dto.IncomeRequest;
import com.smartfinance.backend.ingresos.model.entity.Income;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.ingresos.service.IncomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la importación de extractos bancarios (Sprint 2, Frente 1): {@link #preview} extrae y
 * muestra los movimientos detectados sin persistir nada, y {@link #confirm} crea los
 * ingresos/gastos que el usuario decidió confirmar (posiblemente editados respecto de la
 * previsualización).
 */
@Service
public class StatementImportService {

    /** Margen de días alrededor del rango de fechas extraído, usado para buscar posibles duplicados. */
    private static final int DUPLICATE_WINDOW_DAYS = 3;

    private final StatementTextExtractionService statementTextExtractionService;
    private final StatementAiExtractionService statementAiExtractionService;
    private final DuplicateDetector duplicateDetector;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;

    public StatementImportService(
            StatementTextExtractionService statementTextExtractionService,
            StatementAiExtractionService statementAiExtractionService,
            DuplicateDetector duplicateDetector,
            CategoryRepository categoryRepository,
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            ExpenseService expenseService,
            IncomeService incomeService
    ) {
        this.statementTextExtractionService = statementTextExtractionService;
        this.statementAiExtractionService = statementAiExtractionService;
        this.duplicateDetector = duplicateDetector;
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
    }

    /**
     * Extrae y previsualiza los movimientos de un extracto bancario. No crea ningún
     * {@code Income}/{@code Expense}: el usuario debe revisar y confirmar explícitamente vía
     * {@link #confirm}. No puede marcarse {@code readOnly = true}: internamente registra un
     * {@link com.smartfinance.backend.ia.model.entity.AiUsageEventType#STATEMENT_EXTRACT} vía
     * {@code AiUsageEventService.record}, que hace un INSERT y se ejecuta dentro de esta misma
     * transacción (propagación REQUIRED) — con {@code readOnly = true} Postgres rechaza ese
     * INSERT y toda la operación falla.
     */
    @Transactional
    public StatementPreviewResponse preview(MultipartFile file, String password) {
        Long userId = SecurityUtils.getCurrentUserId();
        validateFile(file);

        String statementText = statementTextExtractionService.extractText(
                file.getOriginalFilename(), readBytes(file), password
        );

        List<Category> incomeCategories = categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(userId, CategoryType.INCOME);
        List<Category> expenseCategories = categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(userId, CategoryType.EXPENSE);

        List<ParsedTransaction> transactions = statementAiExtractionService.extract(
                statementText, incomeCategories, expenseCategories, userId
        );

        List<Boolean> duplicateFlags = detectDuplicates(userId, transactions);

        List<ImportPreviewRow> rows = new ArrayList<>(transactions.size());
        int duplicateCount = 0;
        for (int i = 0; i < transactions.size(); i++) {
            ParsedTransaction transaction = transactions.get(i);
            boolean isDuplicate = duplicateFlags.get(i);
            if (isDuplicate) {
                duplicateCount++;
            }
            rows.add(new ImportPreviewRow(
                    transaction.date(),
                    transaction.description(),
                    transaction.amount(),
                    transaction.movementType(),
                    isDuplicate,
                    transaction.suggestedCategoryId(),
                    transaction.suggestedCategoryName()
            ));
        }

        return new StatementPreviewResponse(rows, rows.size(), duplicateCount);
    }

    /**
     * Crea un ingreso o un gasto por cada fila confirmada por el usuario. La validación de
     * propiedad de {@code categoryId} ocurre dentro de {@link ExpenseService}/{@link IncomeService},
     * igual que en la creación manual de movimientos.
     *
     * @return la cantidad de movimientos creados
     */
    @Transactional
    public StatementImportResultResponse confirm(StatementConfirmRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        int createdCount = 0;
        for (ImportConfirmRow row : request.rows()) {
            if (row.movementType() == MovementType.INCOME) {
                incomeService.createIncome(userId, new IncomeRequest(
                        row.amount(), row.description(), row.date(), row.categoryId()
                ));
            } else {
                PaymentMethodType paymentMethod = row.paymentMethod() != null ? row.paymentMethod() : PaymentMethodType.OTHER;
                expenseService.createExpense(userId, new ExpenseRequest(
                        row.amount(), row.description(), row.date(), paymentMethod, row.categoryId()
                ));
            }
            createdCount++;
        }

        return new StatementImportResultResponse(createdCount);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedStatementFileException("Debe adjuntar un archivo de extracto.");
        }
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new StatementExtractionException("No se pudo leer el archivo subido.", ex);
        }
    }

    /**
     * Trae los ingresos/gastos existentes del usuario en una ventana de
     * {@link #DUPLICATE_WINDOW_DAYS} días alrededor del rango de fechas extraído, y delega en
     * {@link DuplicateDetector} la comparación fila por fila.
     */
    private List<Boolean> detectDuplicates(Long userId, List<ParsedTransaction> transactions) {
        if (transactions.isEmpty()) {
            return List.of();
        }

        LocalDate minDate = transactions.stream().map(ParsedTransaction::date).min(LocalDate::compareTo).orElseThrow();
        LocalDate maxDate = transactions.stream().map(ParsedTransaction::date).max(LocalDate::compareTo).orElseThrow();
        LocalDate windowStart = minDate.minusDays(DUPLICATE_WINDOW_DAYS);
        LocalDate windowEnd = maxDate.plusDays(DUPLICATE_WINDOW_DAYS);

        List<Expense> existingExpenses = expenseRepository.findByUserAndPeriod(userId, windowStart, windowEnd);
        List<Income> existingIncomes = incomeRepository.findByUserAndPeriod(userId, windowStart, windowEnd);

        return duplicateDetector.detectDuplicates(transactions, existingExpenses, existingIncomes);
    }
}
