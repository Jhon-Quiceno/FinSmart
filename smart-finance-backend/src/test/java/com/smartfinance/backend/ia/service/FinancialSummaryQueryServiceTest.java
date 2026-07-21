package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.ia.model.dto.SummaryPeriod;
import com.smartfinance.backend.ia.model.dto.SummaryQueryIntent;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialSummaryQueryServiceTest {

    @Mock
    private AiChatOrchestrator aiChatOrchestrator;

    @Mock
    private AiUsageEventService aiUsageEventService;

    private FinancialSummaryQueryService service;

    @BeforeEach
    void setUp() {
        // JsonMapper es la implementación concreta de ObjectMapper en Jackson 3 (tools.jackson),
        // mismo patrón que AiCategorizationServiceTest para servicios que parsean JSON de IA.
        service = new FinancialSummaryQueryService(aiChatOrchestrator, aiUsageEventService, JsonMapper.builder().build());
    }

    @Test
    void parseQueryReturnsTheDecidedIntentForCleanJsonWithAllFields() {
        when(aiChatOrchestrator.complete(anyList())).thenReturn(new ChatCompletionResult(
                "{\"period\":\"WEEK\",\"movementType\":\"EXPENSE\",\"categoryName\":\"Comida\"}", "groq", "llama-3.3", 10, 5));

        SummaryQueryIntent intent = service.parseQuery(1L, "¿Cuánto gasté en comida esta semana?");

        assertThat(intent.period()).isEqualTo(SummaryPeriod.WEEK);
        assertThat(intent.movementType()).isEqualTo(CategoryType.EXPENSE);
        assertThat(intent.categoryName()).isEqualTo("Comida");
        verify(aiUsageEventService).record(1L, "groq", AiUsageEventType.CATEGORIZE, 15, null);
    }

    @Test
    void parseQueryReturnsNullMovementTypeAndCategoryForABalanceQuestion() {
        when(aiChatOrchestrator.complete(anyList())).thenReturn(new ChatCompletionResult(
                "{\"period\":\"MONTH\",\"movementType\":null,\"categoryName\":null}", "groq", "llama-3.3", null, null));

        SummaryQueryIntent intent = service.parseQuery(2L, "resumen");

        assertThat(intent.period()).isEqualTo(SummaryPeriod.MONTH);
        assertThat(intent.movementType()).isNull();
        assertThat(intent.categoryName()).isNull();
    }

    @Test
    void parseQueryDefaultsToMonthWithNoFilterWhenTheModelResponseIsNotJson() {
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("esto no es JSON", "groq", "llama-3.3", 3, 2));

        SummaryQueryIntent intent = service.parseQuery(3L, "algo raro");

        assertThat(intent).isEqualTo(new SummaryQueryIntent(SummaryPeriod.MONTH, null, null));
    }

    @Test
    void parseQueryDefaultsToMonthWithNoFilterWhenThePeriodValueIsUnrecognized() {
        when(aiChatOrchestrator.complete(anyList())).thenReturn(new ChatCompletionResult(
                "{\"period\":\"NOSEQUE\",\"movementType\":\"EXPENSE\",\"categoryName\":null}", "groq", "llama-3.3", null, null));

        SummaryQueryIntent intent = service.parseQuery(4L, "cuanto gaste");

        assertThat(intent.period()).isEqualTo(SummaryPeriod.MONTH);
        assertThat(intent.movementType()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void parseQueryParsesJsonWrappedInAMarkdownCodeFence() {
        String fenced = """
                ```json
                {"period":"TODAY","movementType":"INCOME","categoryName":null}
                ```
                """;
        when(aiChatOrchestrator.complete(anyList())).thenReturn(new ChatCompletionResult(fenced, "groq", "llama-3.3", null, null));

        SummaryQueryIntent intent = service.parseQuery(5L, "cuanto ingrese hoy");

        assertThat(intent.period()).isEqualTo(SummaryPeriod.TODAY);
        assertThat(intent.movementType()).isEqualTo(CategoryType.INCOME);
    }

    @Test
    void parseQueryDegradesToTheDefaultIntentWithoutThrowingWhenTheAiProviderFails() {
        when(aiChatOrchestrator.complete(anyList()))
                .thenThrow(new AiProviderNotConfiguredException(AiChatOrchestrator.GENERIC_MESSAGE));

        SummaryQueryIntent intent = service.parseQuery(6L, "cuanto gaste");

        assertThat(intent).isEqualTo(new SummaryQueryIntent(SummaryPeriod.MONTH, null, null));
        verify(aiUsageEventService, never()).record(any(), any(), any(), anyInt(), any());
    }

    @Test
    void parseQueryTrimsTheCategoryNameFromTheModelResponse() {
        when(aiChatOrchestrator.complete(anyList())).thenReturn(new ChatCompletionResult(
                "{\"period\":\"MONTH\",\"movementType\":\"EXPENSE\",\"categoryName\":\"  Transporte  \"}",
                "groq", "llama-3.3", null, null));

        SummaryQueryIntent intent = service.parseQuery(7L, "cuanto gaste en transporte");

        assertThat(intent.categoryName()).isEqualTo("Transporte");
    }
}
