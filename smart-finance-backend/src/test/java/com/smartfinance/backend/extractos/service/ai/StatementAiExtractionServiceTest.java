package com.smartfinance.backend.extractos.service.ai;

import tools.jackson.databind.json.JsonMapper;
import com.smartfinance.backend.extractos.exception.StatementExtractionException;
import com.smartfinance.backend.extractos.model.MovementType;
import com.smartfinance.backend.extractos.model.ParsedTransaction;
import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.service.AiUsageEventService;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementAiExtractionServiceTest {

    @Mock
    private AiChatOrchestrator aiChatOrchestrator;

    @Mock
    private AiUsageEventService aiUsageEventService;

    private StatementAiExtractionService service;

    @BeforeEach
    void setUp() {
        // JsonMapper es la implementación concreta de ObjectMapper en Jackson 3 (tools.jackson),
        // el mismo tipo que Spring Boot 4 auto-configura e inyecta en el servicio real.
        service = new StatementAiExtractionService(aiChatOrchestrator, aiUsageEventService, JsonMapper.builder().build());
    }

    @Test
    void extractShouldParseCleanJsonArray() {
        String json = """
                [{"date":"2026-06-01","description":"Compra supermercado","amount":100,"movementType":"EXPENSE","suggestedCategoryName":"Comida"}]
                """;
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(json, "groq", "llama-3.3", 10, 5));

        List<ParsedTransaction> result = service.extract(
                "texto del extracto", List.of(), List.of(buildCategory(1L, "Comida", CategoryType.EXPENSE)), 1L
        );

        Assertions.assertEquals(1, result.size());
        ParsedTransaction transaction = result.get(0);
        Assertions.assertEquals(LocalDate.of(2026, 6, 1), transaction.date());
        Assertions.assertEquals("Compra supermercado", transaction.description());
        Assertions.assertEquals(0, BigDecimal.valueOf(100).compareTo(transaction.amount()));
        Assertions.assertEquals(MovementType.EXPENSE, transaction.movementType());
        Assertions.assertEquals(1L, transaction.suggestedCategoryId());
        Assertions.assertEquals("Comida", transaction.suggestedCategoryName());
    }

    @Test
    void extractShouldParseJsonWrappedInMarkdownCodeFence() {
        String fenced = """
                ```json
                [{"date":"2026-06-01","description":"Salario","amount":2000,"movementType":"INCOME","suggestedCategoryName":null}]
                ```
                """;
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(fenced, "groq", "llama-3.3", null, null));

        List<ParsedTransaction> result = service.extract("texto", List.of(), List.of(), 1L);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(MovementType.INCOME, result.get(0).movementType());
    }

    @Test
    void extractShouldParseJsonSurroundedByProse() {
        String withProse = "Aquí están los movimientos encontrados: "
                + "[{\"date\":\"2026-06-01\",\"description\":\"Compra\",\"amount\":50,\"movementType\":\"EXPENSE\",\"suggestedCategoryName\":null}]"
                + " Espero que sea útil.";
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(withProse, "groq", "llama-3.3", null, null));

        List<ParsedTransaction> result = service.extract("texto", List.of(), List.of(), 1L);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Compra", result.get(0).description());
    }

    @Test
    void extractShouldThrowStatementExtractionExceptionForMalformedJson() {
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("esto no es un arreglo JSON válido", "groq", "llama-3.3", null, null));

        Assertions.assertThrows(StatementExtractionException.class, () -> service.extract("texto", List.of(), List.of(), 1L));
    }

    @Test
    void extractShouldDropRowsWithInvalidDateAmountOrMovementType() {
        String json = """
                [
                  {"date":"fecha-invalida","description":"Fila con fecha invalida","amount":100,"movementType":"EXPENSE","suggestedCategoryName":null},
                  {"date":"2026-06-01","description":"Fila con monto nulo","amount":null,"movementType":"EXPENSE","suggestedCategoryName":null},
                  {"date":"2026-06-01","description":"Fila con tipo invalido","amount":100,"movementType":"NOSEQUE","suggestedCategoryName":null},
                  {"date":"2026-06-01","description":"Fila valida","amount":-100,"movementType":"EXPENSE","suggestedCategoryName":null}
                ]
                """;
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(json, "groq", "llama-3.3", null, null));

        List<ParsedTransaction> result = service.extract("texto", List.of(), List.of(), 1L);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Fila valida", result.get(0).description());
        Assertions.assertEquals(0, BigDecimal.valueOf(100).compareTo(result.get(0).amount()));
    }

    @Test
    void extractShouldFallBackToSlashDateFormat() {
        String json = """
                [{"date":"15/06/2026","description":"Compra","amount":100,"movementType":"EXPENSE","suggestedCategoryName":null}]
                """;
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(json, "groq", "llama-3.3", null, null));

        List<ParsedTransaction> result = service.extract("texto", List.of(), List.of(), 1L);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(LocalDate.of(2026, 6, 15), result.get(0).date());
    }

    @Test
    void extractShouldResolveSuggestedCategoryCaseInsensitively() {
        String json = """
                [{"date":"2026-06-01","description":"Compra","amount":100,"movementType":"EXPENSE","suggestedCategoryName":"comida"}]
                """;
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(json, "groq", "llama-3.3", null, null));

        List<ParsedTransaction> result = service.extract(
                "texto", List.of(), List.of(buildCategory(5L, "Comida", CategoryType.EXPENSE)), 1L
        );

        Assertions.assertEquals(5L, result.get(0).suggestedCategoryId());
        Assertions.assertEquals("Comida", result.get(0).suggestedCategoryName());
    }

    @Test
    void extractShouldReturnNullCategoryWhenSuggestedNameDoesNotMatchAnyCategory() {
        String json = """
                [{"date":"2026-06-01","description":"Compra","amount":100,"movementType":"EXPENSE","suggestedCategoryName":"Categoria inexistente"}]
                """;
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(json, "groq", "llama-3.3", null, null));

        List<ParsedTransaction> result = service.extract(
                "texto", List.of(), List.of(buildCategory(5L, "Comida", CategoryType.EXPENSE)), 1L
        );

        Assertions.assertNull(result.get(0).suggestedCategoryId());
        Assertions.assertNull(result.get(0).suggestedCategoryName());
    }

    @Test
    void extractShouldRecordUsageEventWithStatementExtractType() {
        String json = "[]";
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(json, "nvidia", "llama-3.3", 20, 15));

        service.extract("texto", List.of(), List.of(), 7L);

        verify(aiUsageEventService).record(7L, "nvidia", AiUsageEventType.STATEMENT_EXTRACT, 35, null);
    }

    private static Category buildCategory(Long id, String name, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        return category;
    }
}
