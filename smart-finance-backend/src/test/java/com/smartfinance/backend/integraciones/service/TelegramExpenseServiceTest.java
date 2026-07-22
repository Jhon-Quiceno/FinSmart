package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.deudas.model.entity.Debt;
import com.smartfinance.backend.deudas.repository.DebtRepository;
import com.smartfinance.backend.gastos.model.dto.ExpenseRequest;
import com.smartfinance.backend.gastos.model.dto.ExpenseResponse;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.repository.CategoryTotalProjection;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.gastos.service.ExpenseService;
import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.ia.model.dto.MovementClassification;
import com.smartfinance.backend.ia.model.dto.ReceiptExtraction;
import com.smartfinance.backend.ia.model.dto.SummaryPeriod;
import com.smartfinance.backend.ia.model.dto.SummaryQueryIntent;
import com.smartfinance.backend.ia.model.dto.SummaryTopic;
import com.smartfinance.backend.ia.service.AiCategorizationService;
import com.smartfinance.backend.ia.service.FinancialSummaryQueryService;
import com.smartfinance.backend.ia.service.ReceiptExtractionService;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ingresos.model.dto.IncomeRequest;
import com.smartfinance.backend.ingresos.model.dto.IncomeResponse;
import com.smartfinance.backend.ingresos.model.entity.Income;
import com.smartfinance.backend.ingresos.repository.IncomeCategoryTotalProjection;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.ingresos.service.IncomeService;
import com.smartfinance.backend.integraciones.exception.TelegramChatNotLinkedException;
import com.smartfinance.backend.integraciones.exception.TelegramImplausibleMovementException;
import com.smartfinance.backend.integraciones.exception.TelegramRateLimitExceededException;
import com.smartfinance.backend.integraciones.model.entity.TelegramLink;
import com.smartfinance.backend.integraciones.repository.TelegramLinkRepository;
import com.smartfinance.backend.usuario.model.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramExpenseServiceTest {

    @Mock
    private TelegramLinkRepository telegramLinkRepository;

    @Mock
    private AiCategorizationService aiCategorizationService;

    @Mock
    private ReceiptExtractionService receiptExtractionService;

    @Mock
    private FinancialSummaryQueryService financialSummaryQueryService;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private IncomeService incomeService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private DebtRepository debtRepository;

    private final TelegramMessageParser messageParser = new TelegramMessageParser();

    private TelegramExpenseService telegramExpenseService;

    /**
     * Default sin duplicados: se ejecuta ANTES del cuerpo de cada test (a diferencia de un stub
     * puesto dentro de {@link #service()}, que se registraría después de los {@code when(...)}
     * propios de cada test y los pisaría, según el orden "el último stub gana" de Mockito para
     * matchers que se superponen).
     */
    @BeforeEach
    void stubEmptyDuplicateCandidatesByDefault() {
        lenient().when(expenseRepository.findByUserAndPeriod(anyLong(), any(), any())).thenReturn(List.of());
        lenient().when(incomeRepository.findByUserAndPeriod(anyLong(), any(), any())).thenReturn(List.of());
    }

    private TelegramExpenseService service() {
        return new TelegramExpenseService(
                telegramLinkRepository, messageParser, aiCategorizationService, receiptExtractionService,
                financialSummaryQueryService, expenseService, incomeService, expenseRepository, incomeRepository,
                debtRepository
        );
    }

    private TelegramLink linkFor(Long userId, String chatId) {
        User user = new User();
        user.setId(userId);
        TelegramLink link = new TelegramLink();
        link.setTelegramChatId(chatId);
        link.setUser(user);
        return link;
    }

    @Test
    void registerFromMessageThrowsWhenChatIsNotLinked() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.empty());
        telegramExpenseService = service();

        Assertions.assertThrows(
                TelegramChatNotLinkedException.class,
                () -> telegramExpenseService.registerFromMessage("chat-1", "Uber 15000")
        );
    }

    @Test
    void registerFromMessagePropagatesTheSanityFilterRejectionWithoutSwallowingIt() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        telegramExpenseService = service();

        Assertions.assertThrows(
                TelegramImplausibleMovementException.class,
                () -> telegramExpenseService.registerFromMessage("chat-1", "15000 12345")
        );
        verify(aiCategorizationService, never()).classifyMovement(any(), any(), any());
        verify(expenseService, never()).createExpense(any(), any());
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void registerFromMessageCreatesExpenseWithTheCategorySuggestedByAi() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(aiCategorizationService.classifyMovement(eq(7L), eq("Uber"), any(BigDecimal.class)))
                .thenReturn(new MovementClassification(CategoryType.EXPENSE, 4L, "Transporte"));
        ExpenseResponse createdExpense = new ExpenseResponse(
                1L, BigDecimal.valueOf(15000), "Uber", LocalDate.now(), PaymentMethodType.OTHER, 4L, "Transporte", null
        );
        when(expenseService.createExpense(eq(7L), any(ExpenseRequest.class))).thenReturn(createdExpense);
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "Uber 15000");

        ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
        verify(expenseService).createExpense(eq(7L), captor.capture());
        verify(incomeService, never()).createIncome(any(), any());
        assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(captor.getValue().description()).isEqualTo("Uber");
        assertThat(captor.getValue().paymentMethod()).isEqualTo(PaymentMethodType.OTHER);
        assertThat(captor.getValue().categoryId()).isEqualTo(4L);
        assertThat(reply).isEqualTo("✅ Gasto registrado: Uber — $15.000 (Transporte)");
    }

    @Test
    void registerFromMessageCreatesIncomeWhenAiClassifiesItAsIncome() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(aiCategorizationService.classifyMovement(eq(7L), eq("Me pagaron"), any(BigDecimal.class)))
                .thenReturn(new MovementClassification(CategoryType.INCOME, 9L, "Salario"));
        IncomeResponse createdIncome = new IncomeResponse(1L, BigDecimal.valueOf(50000), "Me pagaron", LocalDate.now(), 9L, "Salario");
        when(incomeService.createIncome(eq(7L), any(IncomeRequest.class))).thenReturn(createdIncome);
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "Me pagaron 50000");

        ArgumentCaptor<IncomeRequest> captor = ArgumentCaptor.forClass(IncomeRequest.class);
        verify(incomeService).createIncome(eq(7L), captor.capture());
        verify(expenseService, never()).createExpense(any(), any());
        assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(captor.getValue().description()).isEqualTo("Me pagaron");
        assertThat(captor.getValue().categoryId()).isEqualTo(9L);
        assertThat(reply).isEqualTo("✅ Ingreso registrado: Me pagaron — $50.000 (Salario)");
    }

    @Test
    void registerFromMessageStillCreatesExpenseWithoutCategoryWhenAiFails() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(aiCategorizationService.classifyMovement(anyLong(), any(), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("AI provider down"));
        ExpenseResponse createdExpense = new ExpenseResponse(
                1L, BigDecimal.valueOf(15000), "Uber", LocalDate.now(), PaymentMethodType.OTHER, null, null, null
        );
        when(expenseService.createExpense(eq(7L), any(ExpenseRequest.class))).thenReturn(createdExpense);
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "Uber 15000");

        ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
        verify(expenseService).createExpense(eq(7L), captor.capture());
        verify(incomeService, never()).createIncome(any(), any());
        assertThat(captor.getValue().categoryId()).isNull();
        assertThat(reply).isEqualTo("✅ Gasto registrado: Uber — $15.000 (sin categoría)");
    }

    @Test
    void registerFromMessageThrowsRateLimitExceededAfterTooManyMessagesFromTheSameChat() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(aiCategorizationService.classifyMovement(anyLong(), any(), any(BigDecimal.class)))
                .thenReturn(new MovementClassification(CategoryType.EXPENSE, null, null));
        when(expenseService.createExpense(eq(7L), any(ExpenseRequest.class))).thenReturn(
                new ExpenseResponse(1L, BigDecimal.valueOf(15000), "Uber", LocalDate.now(), PaymentMethodType.OTHER, null, null, null)
        );
        telegramExpenseService = service();

        for (int i = 0; i < 10; i++) {
            telegramExpenseService.registerFromMessage("chat-1", "Uber 15000");
        }

        Assertions.assertThrows(
                TelegramRateLimitExceededException.class,
                () -> telegramExpenseService.registerFromMessage("chat-1", "Uber 15000")
        );
    }

    @Test
    void registerFromMessageWarnsAboutAPossibleDuplicateExpenseFromTheSameDay() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(aiCategorizationService.classifyMovement(eq(7L), eq("Uber"), any(BigDecimal.class)))
                .thenReturn(new MovementClassification(CategoryType.EXPENSE, null, null));
        when(expenseService.createExpense(eq(7L), any(ExpenseRequest.class))).thenReturn(
                new ExpenseResponse(1L, BigDecimal.valueOf(15000), "Uber", LocalDate.now(), PaymentMethodType.OTHER, null, null, null)
        );
        Expense existing = new Expense();
        existing.setAmount(BigDecimal.valueOf(15000));
        existing.setDescription("Uber");
        when(expenseRepository.findByUserAndPeriod(eq(7L), any(), any())).thenReturn(List.of(existing));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "Uber 15000");

        assertThat(reply).contains("⚠️ Parece similar a un gasto que ya registraste hoy");
    }

    @Test
    void registerFromMessageWarnsAboutAPossibleDuplicateIncomeFromTheSameDay() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(aiCategorizationService.classifyMovement(eq(7L), eq("Me pagaron"), any(BigDecimal.class)))
                .thenReturn(new MovementClassification(CategoryType.INCOME, null, null));
        when(incomeService.createIncome(eq(7L), any(IncomeRequest.class))).thenReturn(
                new IncomeResponse(1L, BigDecimal.valueOf(50000), "Me pagaron", LocalDate.now(), null, null)
        );
        Income existing = new Income();
        existing.setAmount(BigDecimal.valueOf(50000));
        existing.setDescription("Me pagaron");
        when(incomeRepository.findByUserAndPeriod(eq(7L), any(), any())).thenReturn(List.of(existing));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "Me pagaron 50000");

        assertThat(reply).contains("⚠️ Parece similar a un ingreso que ya registraste hoy");
    }

    @Test
    void registerFromMessageDoesNotWarnWhenNoSimilarMovementExistsToday() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(aiCategorizationService.classifyMovement(eq(7L), eq("Uber"), any(BigDecimal.class)))
                .thenReturn(new MovementClassification(CategoryType.EXPENSE, null, null));
        when(expenseService.createExpense(eq(7L), any(ExpenseRequest.class))).thenReturn(
                new ExpenseResponse(1L, BigDecimal.valueOf(15000), "Uber", LocalDate.now(), PaymentMethodType.OTHER, null, null, null)
        );
        Expense unrelated = new Expense();
        unrelated.setAmount(BigDecimal.valueOf(90000));
        unrelated.setDescription("Mercado");
        when(expenseRepository.findByUserAndPeriod(eq(7L), any(), any())).thenReturn(List.of(unrelated));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "Uber 15000");

        assertThat(reply).doesNotContain("⚠️");
    }

    @Test
    void registerFromPhotoThrowsWhenChatIsNotLinked() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.empty());
        telegramExpenseService = service();

        Assertions.assertThrows(
                TelegramChatNotLinkedException.class,
                () -> telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,abc")
        );
    }

    @Test
    void registerFromPhotoReturnsFriendlyMessageAndCreatesNothingWhenImageIsNotAReceipt() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(receiptExtractionService.extractFromImage(7L, "data:image/jpeg;base64,cat"))
                .thenReturn(ReceiptExtraction.notAReceipt());
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,cat");

        assertThat(reply).contains("No pude leer un recibo en esa imagen");
        verify(expenseService, never()).createExpense(any(), any());
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void registerFromPhotoCreatesExpenseWithTheDataExtractedFromTheReceipt() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(receiptExtractionService.extractFromImage(7L, "data:image/jpeg;base64,tesco"))
                .thenReturn(new ReceiptExtraction(true, "TESCO", BigDecimal.valueOf(6710), CategoryType.EXPENSE, 4L, "Supermercado"));
        ExpenseResponse createdExpense = new ExpenseResponse(
                1L, BigDecimal.valueOf(6710), "TESCO", LocalDate.now(), PaymentMethodType.OTHER, 4L, "Supermercado", null
        );
        when(expenseService.createExpense(eq(7L), any(ExpenseRequest.class))).thenReturn(createdExpense);
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,tesco");

        ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
        verify(expenseService).createExpense(eq(7L), captor.capture());
        verify(incomeService, never()).createIncome(any(), any());
        assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(6710));
        assertThat(captor.getValue().description()).isEqualTo("TESCO");
        assertThat(captor.getValue().paymentMethod()).isEqualTo(PaymentMethodType.OTHER);
        assertThat(captor.getValue().categoryId()).isEqualTo(4L);
        assertThat(reply).isEqualTo("✅ Gasto registrado desde la foto: TESCO — $6.710 (Supermercado)");
    }

    @Test
    void registerFromPhotoCreatesIncomeWhenExtractionDecidesItIsAnIncome() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(receiptExtractionService.extractFromImage(7L, "data:image/jpeg;base64,refund"))
                .thenReturn(new ReceiptExtraction(true, "Nota de crédito", BigDecimal.valueOf(20000), CategoryType.INCOME, 9L, "Reembolso"));
        IncomeResponse createdIncome = new IncomeResponse(1L, BigDecimal.valueOf(20000), "Nota de crédito", LocalDate.now(), 9L, "Reembolso");
        when(incomeService.createIncome(eq(7L), any(IncomeRequest.class))).thenReturn(createdIncome);
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,refund");

        ArgumentCaptor<IncomeRequest> captor = ArgumentCaptor.forClass(IncomeRequest.class);
        verify(incomeService).createIncome(eq(7L), captor.capture());
        verify(expenseService, never()).createExpense(any(), any());
        assertThat(captor.getValue().categoryId()).isEqualTo(9L);
        assertThat(reply).isEqualTo("✅ Ingreso registrado desde la foto: Nota de crédito — $20.000 (Reembolso)");
    }

    @Test
    void registerFromPhotoRejectsAnImplausiblyLowAmountWithoutCreatingAnything() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(receiptExtractionService.extractFromImage(7L, "data:image/jpeg;base64,abc"))
                .thenReturn(new ReceiptExtraction(true, "Kiosco", BigDecimal.valueOf(1), CategoryType.EXPENSE, null, null));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,abc");

        assertThat(reply).contains("parece muy bajo");
        verify(expenseService, never()).createExpense(any(), any());
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void registerFromPhotoRejectsAnImplausiblyHighAmountWithoutCreatingAnything() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(receiptExtractionService.extractFromImage(7L, "data:image/jpeg;base64,abc"))
                .thenReturn(new ReceiptExtraction(true, "Casa", BigDecimal.valueOf(999_999_999_999L), CategoryType.EXPENSE, null, null));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,abc");

        assertThat(reply).contains("parece demasiado alto");
        verify(expenseService, never()).createExpense(any(), any());
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void registerFromPhotoDegradesToTheNotAReceiptReplyWithoutCreatingAnythingWhenExtractionThrows() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(receiptExtractionService.extractFromImage(7L, "data:image/jpeg;base64,abc"))
                .thenThrow(new AiProviderNotConfiguredException(AiChatOrchestrator.GENERIC_MESSAGE));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,abc");

        assertThat(reply).contains("No pude leer un recibo en esa imagen");
        verify(expenseService, never()).createExpense(any(), any());
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void registerFromPhotoWarnsAboutAPossibleDuplicateExpenseFromTheSameDay() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(receiptExtractionService.extractFromImage(7L, "data:image/jpeg;base64,abc"))
                .thenReturn(new ReceiptExtraction(true, "Uber", BigDecimal.valueOf(15000), CategoryType.EXPENSE, null, null));
        when(expenseService.createExpense(eq(7L), any(ExpenseRequest.class))).thenReturn(
                new ExpenseResponse(1L, BigDecimal.valueOf(15000), "Uber", LocalDate.now(), PaymentMethodType.OTHER, null, null, null)
        );
        Expense existing = new Expense();
        existing.setAmount(BigDecimal.valueOf(15000));
        existing.setDescription("Uber");
        when(expenseRepository.findByUserAndPeriod(eq(7L), any(), any())).thenReturn(List.of(existing));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,abc");

        assertThat(reply).contains("⚠️ Parece similar a un gasto que ya registraste hoy");
    }

    @Test
    void rateLimitBudgetIsSharedBetweenTextMessagesAndPhotosFromTheSameChat() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(aiCategorizationService.classifyMovement(anyLong(), any(), any(BigDecimal.class)))
                .thenReturn(new MovementClassification(CategoryType.EXPENSE, null, null));
        when(expenseService.createExpense(eq(7L), any(ExpenseRequest.class))).thenReturn(
                new ExpenseResponse(1L, BigDecimal.valueOf(15000), "Uber", LocalDate.now(), PaymentMethodType.OTHER, null, null, null)
        );
        telegramExpenseService = service();

        for (int i = 0; i < 10; i++) {
            telegramExpenseService.registerFromMessage("chat-1", "Uber 15000");
        }

        Assertions.assertThrows(
                TelegramRateLimitExceededException.class,
                () -> telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,abc")
        );
        verify(receiptExtractionService, never()).extractFromImage(any(), any());
    }

    @Test
    void registerFromMessageRoutesQueryLookingTextToTheSummaryPathWithoutRegisteringAnything() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("¿Cuánto gasté en comida?")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.MONTH, CategoryType.EXPENSE, "Comida", SummaryTopic.MOVEMENT));
        CategoryTotalProjection comida = mock(CategoryTotalProjection.class);
        when(comida.getCategoryName()).thenReturn("Comida");
        when(comida.getTotal()).thenReturn(BigDecimal.valueOf(180500));
        when(expenseRepository.findTopCategoriesByUserAndPeriod(eq(7L), any(), any())).thenReturn(List.of(comida));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "¿Cuánto gasté en comida?");

        assertThat(reply).isEqualTo("📊 Gastaste $180.500 en Comida este mes.");
        verify(aiCategorizationService, never()).classifyMovement(any(), any(), any());
        verify(expenseService, never()).createExpense(any(), any());
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void registerFromMessageRepliesWithoutErrorWhenTheAskedCategoryHasNoExpensesInThePeriod() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("cuanto gaste en mascotas")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.MONTH, CategoryType.EXPENSE, "Mascotas", SummaryTopic.MOVEMENT));
        when(expenseRepository.findTopCategoriesByUserAndPeriod(eq(7L), any(), any())).thenReturn(List.of());
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "cuanto gaste en mascotas");

        assertThat(reply).isEqualTo("📊 No encontré gastos en \"Mascotas\" este mes.");
        verify(expenseService, never()).createExpense(any(), any());
    }

    @Test
    void registerFromMessageBuildsAnOverallExpenseTotalReplyWhenNoCategoryIsMentioned() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("cuanto gaste en total esta semana")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.WEEK, CategoryType.EXPENSE, null, SummaryTopic.MOVEMENT));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(7L), any(), any())).thenReturn(BigDecimal.valueOf(1250000));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "cuanto gaste en total esta semana");

        assertThat(reply).isEqualTo("📊 Gastaste $1.250.000 en total esta semana.");
    }

    @Test
    void registerFromMessageBuildsAnIncomeCategoryBreakdownReplyWhenTheCategoryMatches() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("cuanto ingrese por freelance")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.MONTH, CategoryType.INCOME, "Freelance", SummaryTopic.MOVEMENT));
        IncomeCategoryTotalProjection freelance = mock(IncomeCategoryTotalProjection.class);
        when(freelance.getCategoryName()).thenReturn("Freelance");
        when(freelance.getTotal()).thenReturn(BigDecimal.valueOf(2850000));
        when(incomeRepository.findTopCategoriesByUserAndPeriod(eq(7L), any(), any())).thenReturn(List.of(freelance));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "cuanto ingrese por freelance");

        assertThat(reply).isEqualTo("📊 Ingresaste $2.850.000 en Freelance este mes.");
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void registerFromMessageRepliesWithoutErrorWhenTheAskedIncomeCategoryHasNoIncomeInThePeriod() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("cuanto ingrese por consultoria")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.MONTH, CategoryType.INCOME, "Consultoria", SummaryTopic.MOVEMENT));
        when(incomeRepository.findTopCategoriesByUserAndPeriod(eq(7L), any(), any())).thenReturn(List.of());
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "cuanto ingrese por consultoria");

        assertThat(reply).isEqualTo("📊 No encontré ingresos en \"Consultoria\" este mes.");
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void registerFromMessageBuildsABalanceReplyForAGeneralSummaryQuestion() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("resumen")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.MONTH, null, null, SummaryTopic.MOVEMENT));
        when(incomeRepository.sumAmountByUserAndPeriod(eq(7L), any(), any())).thenReturn(BigDecimal.valueOf(2850000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(7L), any(), any())).thenReturn(BigDecimal.valueOf(1400000));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "resumen");

        assertThat(reply).isEqualTo("📊 Balance de este mes: ingresos $2.850.000, gastos $1.400.000 (neto: +$1.450.000).");
        verify(expenseService, never()).createExpense(any(), any());
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void registerFromMessageBuildsABalanceReplyWithANegativeNetWhenExpensesExceedIncome() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("balance")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.MONTH, null, null, SummaryTopic.MOVEMENT));
        when(incomeRepository.sumAmountByUserAndPeriod(eq(7L), any(), any())).thenReturn(BigDecimal.valueOf(500000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(7L), any(), any())).thenReturn(BigDecimal.valueOf(800000));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "balance");

        assertThat(reply).isEqualTo("📊 Balance de este mes: ingresos $500.000, gastos $800.000 (neto: -$300.000).");
    }

    private static Debt debtWithRemaining(BigDecimal remainingAmount) {
        Debt debt = new Debt();
        debt.setRemainingAmount(remainingAmount);
        return debt;
    }

    @Test
    void registerFromMessageBuildsADebtSummaryReplyWhenTheTopicIsDebt() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("¿cómo voy con mis deudas?")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.MONTH, null, null, SummaryTopic.DEBT));
        when(debtRepository.findAllByUser_Id(7L)).thenReturn(List.of(
                debtWithRemaining(BigDecimal.valueOf(300000)),
                debtWithRemaining(BigDecimal.valueOf(0)),
                debtWithRemaining(BigDecimal.valueOf(150000))
        ));
        when(debtRepository.sumRemainingAmountByUser(7L)).thenReturn(BigDecimal.valueOf(450000));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "¿cómo voy con mis deudas?");

        assertThat(reply).isEqualTo("💳 Tenés $450.000 en deudas pendientes, en 2 deuda(s) activa(s).");
        verify(expenseRepository, never()).sumAmountByUserAndPeriod(any(), any(), any());
        verify(incomeRepository, never()).sumAmountByUserAndPeriod(any(), any(), any());
    }

    @Test
    void registerFromMessageRepliesThatThereAreNoDebtsWhenNoneAreOutstanding() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("cuanto debo")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.MONTH, null, null, SummaryTopic.DEBT));
        when(debtRepository.findAllByUser_Id(7L)).thenReturn(List.of(debtWithRemaining(BigDecimal.ZERO)));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "cuanto debo");

        assertThat(reply).isEqualTo("🎉 No tenés deudas pendientes registradas.");
        verify(debtRepository, never()).sumRemainingAmountByUser(any());
    }

    @Test
    void registerFromMessageBuildsAnExpenseSummaryReplyForTheLastMonthPeriod() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("cuanto gaste el mes pasado")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.LAST_MONTH, CategoryType.EXPENSE, null, SummaryTopic.MOVEMENT));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(7L), any(), any())).thenReturn(BigDecimal.valueOf(900000));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "cuanto gaste el mes pasado");

        assertThat(reply).isEqualTo("📊 Gastaste $900.000 en total el mes pasado.");
        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(expenseRepository).sumAmountByUserAndPeriod(eq(7L), startCaptor.capture(), endCaptor.capture());
        LocalDate expectedEnd = LocalDate.now().withDayOfMonth(1).minusDays(1);
        LocalDate expectedStart = expectedEnd.withDayOfMonth(1);
        assertThat(startCaptor.getValue()).isEqualTo(expectedStart);
        assertThat(endCaptor.getValue()).isEqualTo(expectedEnd);
    }

    @Test
    void registerFromMessageBuildsAnExpenseSummaryReplyForTheYearPeriod() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(financialSummaryQueryService.parseQuery(eq(7L), eq("cuanto gaste este año")))
                .thenReturn(new SummaryQueryIntent(SummaryPeriod.YEAR, CategoryType.EXPENSE, null, SummaryTopic.MOVEMENT));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(7L), any(), any())).thenReturn(BigDecimal.valueOf(12000000));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "cuanto gaste este año");

        assertThat(reply).isEqualTo("📊 Gastaste $12.000.000 en total este año.");
        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(expenseRepository).sumAmountByUserAndPeriod(eq(7L), startCaptor.capture(), eq(LocalDate.now()));
        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.now().withDayOfYear(1));
    }

    @Test
    void registerFromMessageRepliesWithAFriendlyHelpMessageWhenTheTextHasNoInterpretableAmount() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        telegramExpenseService = service();

        String reply = telegramExpenseService.registerFromMessage("chat-1", "hola");

        assertThat(reply).contains("No entendí ese mensaje");
        verify(aiCategorizationService, never()).classifyMovement(any(), any(), any());
        verify(expenseService, never()).createExpense(any(), any());
        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    void rateLimitBudgetIsSharedBetweenRegistrationMessagesAndSummaryQueriesFromTheSameChat() {
        when(telegramLinkRepository.findByTelegramChatId("chat-1")).thenReturn(Optional.of(linkFor(7L, "chat-1")));
        when(aiCategorizationService.classifyMovement(anyLong(), any(), any(BigDecimal.class)))
                .thenReturn(new MovementClassification(CategoryType.EXPENSE, null, null));
        when(expenseService.createExpense(eq(7L), any(ExpenseRequest.class))).thenReturn(
                new ExpenseResponse(1L, BigDecimal.valueOf(15000), "Uber", LocalDate.now(), PaymentMethodType.OTHER, null, null, null)
        );
        telegramExpenseService = service();

        for (int i = 0; i < 10; i++) {
            telegramExpenseService.registerFromMessage("chat-1", "Uber 15000");
        }

        Assertions.assertThrows(
                TelegramRateLimitExceededException.class,
                () -> telegramExpenseService.registerFromMessage("chat-1", "resumen")
        );
        verify(financialSummaryQueryService, never()).parseQuery(any(), any());
    }
}
