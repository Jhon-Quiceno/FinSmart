package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.gastos.model.dto.ExpenseRequest;
import com.smartfinance.backend.gastos.model.dto.ExpenseResponse;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.gastos.service.ExpenseService;
import com.smartfinance.backend.ia.model.dto.MovementClassification;
import com.smartfinance.backend.ia.service.AiCategorizationService;
import com.smartfinance.backend.ingresos.model.dto.IncomeRequest;
import com.smartfinance.backend.ingresos.model.dto.IncomeResponse;
import com.smartfinance.backend.ingresos.service.IncomeService;
import com.smartfinance.backend.integraciones.exception.TelegramChatNotLinkedException;
import com.smartfinance.backend.integraciones.exception.TelegramImplausibleMovementException;
import com.smartfinance.backend.integraciones.model.entity.TelegramLink;
import com.smartfinance.backend.integraciones.repository.TelegramLinkRepository;
import com.smartfinance.backend.usuario.model.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    private ExpenseService expenseService;

    @Mock
    private IncomeService incomeService;

    private final TelegramMessageParser messageParser = new TelegramMessageParser();

    private TelegramExpenseService telegramExpenseService;

    private TelegramExpenseService service() {
        return new TelegramExpenseService(telegramLinkRepository, messageParser, aiCategorizationService, expenseService, incomeService);
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
}
