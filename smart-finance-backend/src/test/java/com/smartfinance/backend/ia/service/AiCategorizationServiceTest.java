package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.ia.model.dto.CategorizeRequest;
import com.smartfinance.backend.ia.model.dto.CategorizeResponse;
import com.smartfinance.backend.ia.model.dto.MovementClassification;
import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.repository.CategoryRepository;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCategorizationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AiChatOrchestrator aiChatOrchestrator;

    @Mock
    private AiUsageEventService aiUsageEventService;

    private AiCategorizationService service;

    @BeforeEach
    void setUp() {
        // JsonMapper es la implementación concreta de ObjectMapper en Jackson 3 (tools.jackson),
        // el mismo tipo que Spring Boot 4 auto-configura e inyecta en el servicio real (ver
        // StatementAiExtractionServiceTest, mismo patrón para servicios que parsean JSON de IA).
        service = new AiCategorizationService(categoryRepository, aiChatOrchestrator, aiUsageEventService, JsonMapper.builder().build());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void categorizeShouldReturnNullMatchWhenUserHasNoExpenseCategories() {
        setAuthenticatedUser(1L);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(1L, CategoryType.EXPENSE)).thenReturn(List.of());

        CategorizeResponse response = service.categorize(new CategorizeRequest("Almuerzo en restaurante", null, CategoryType.EXPENSE));

        Assertions.assertNull(response.categoryId());
        Assertions.assertNull(response.categoryName());
        verify(aiChatOrchestrator, never()).complete(anyList());
    }

    @Test
    void categorizeShouldThrowWhenNoProviderIsConfigured() {
        setAuthenticatedUser(2L);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(2L, CategoryType.EXPENSE))
                .thenReturn(List.of(buildCategory(1L, "Comida", CategoryType.EXPENSE)));
        when(aiChatOrchestrator.complete(anyList()))
                .thenThrow(new AiProviderNotConfiguredException(AiChatOrchestrator.GENERIC_MESSAGE));

        Assertions.assertThrows(AiProviderNotConfiguredException.class, () ->
                service.categorize(new CategorizeRequest("Almuerzo", null, CategoryType.EXPENSE))
        );
        verify(aiUsageEventService, never()).record(any(), any(), any(), anyInt(), any());
    }

    @Test
    void categorizeShouldReturnExactMatchWhenModelRespondsWithExactCategoryName() {
        setAuthenticatedUser(3L);
        Category comida = buildCategory(1L, "Comida", CategoryType.EXPENSE);
        Category transporte = buildCategory(2L, "Transporte", CategoryType.EXPENSE);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(3L, CategoryType.EXPENSE))
                .thenReturn(List.of(comida, transporte));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("Comida", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Almuerzo en restaurante", BigDecimal.valueOf(25000), CategoryType.EXPENSE));

        Assertions.assertEquals(1L, response.categoryId());
        Assertions.assertEquals("Comida", response.categoryName());
        verify(aiUsageEventService).record(3L, "groq", AiUsageEventType.CATEGORIZE, 0, null);
    }

    @Test
    void categorizeShouldTolerateExtraChatterAroundTheCategoryName() {
        setAuthenticatedUser(4L);
        Category transporte = buildCategory(2L, "Transporte", CategoryType.EXPENSE);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(4L, CategoryType.EXPENSE))
                .thenReturn(List.of(transporte));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("La categoría es Transporte.", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Uber al trabajo", null, CategoryType.EXPENSE));

        Assertions.assertEquals(2L, response.categoryId());
        Assertions.assertEquals("Transporte", response.categoryName());
    }

    @Test
    void categorizeShouldReturnNullMatchWhenModelRespondsWithNoMatchToken() {
        setAuthenticatedUser(5L);
        Category comida = buildCategory(1L, "Comida", CategoryType.EXPENSE);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(5L, CategoryType.EXPENSE))
                .thenReturn(List.of(comida));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("NINGUNA", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Pago de arriendo", null, CategoryType.EXPENSE));

        Assertions.assertNull(response.categoryId());
        Assertions.assertNull(response.categoryName());
    }

    @Test
    void categorizeShouldMatchCaseInsensitively() {
        setAuthenticatedUser(6L);
        Category comida = buildCategory(1L, "Comida", CategoryType.EXPENSE);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(6L, CategoryType.EXPENSE))
                .thenReturn(List.of(comida));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("comida", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Mercado", null, CategoryType.EXPENSE));

        Assertions.assertEquals(1L, response.categoryId());
    }

    @Test
    void categorizeShouldDefaultToExpenseTypeWhenTypeIsNotProvided() {
        setAuthenticatedUser(7L);
        Category comida = buildCategory(1L, "Comida", CategoryType.EXPENSE);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(7L, CategoryType.EXPENSE))
                .thenReturn(List.of(comida));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("Comida", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Almuerzo", null, null));

        Assertions.assertEquals(1L, response.categoryId());
        verify(categoryRepository).findAllByUser_IdAndTypeOrderByNameAsc(7L, CategoryType.EXPENSE);
    }

    @Test
    void categorizeShouldLoadIncomeCategoriesWhenTypeIsIncome() {
        setAuthenticatedUser(8L);
        Category salario = buildCategory(3L, "Salario", CategoryType.INCOME);
        Category freelance = buildCategory(4L, "Freelance", CategoryType.INCOME);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(8L, CategoryType.INCOME))
                .thenReturn(List.of(salario, freelance));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("Salario", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Pago mensual de nomina", BigDecimal.valueOf(2500000), CategoryType.INCOME));

        Assertions.assertEquals(3L, response.categoryId());
        Assertions.assertEquals("Salario", response.categoryName());
        verify(categoryRepository, never()).findAllByUser_IdAndTypeOrderByNameAsc(8L, CategoryType.EXPENSE);
    }

    @Test
    void categorizeShouldReturnNullMatchWhenUserHasNoIncomeCategories() {
        setAuthenticatedUser(9L);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(9L, CategoryType.INCOME)).thenReturn(List.of());

        CategorizeResponse response = service.categorize(new CategorizeRequest("Venta de un mueble", null, CategoryType.INCOME));

        Assertions.assertNull(response.categoryId());
        Assertions.assertNull(response.categoryName());
        verify(aiChatOrchestrator, never()).complete(anyList());
    }

    @Test
    void classifyMovementShouldReturnIncomeWithMatchedCategoryForCleanJson() {
        Category salario = buildCategory(3L, "Salario", CategoryType.INCOME);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(10L, CategoryType.INCOME))
                .thenReturn(List.of(salario));
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(10L, CategoryType.EXPENSE))
                .thenReturn(List.of());
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(
                        "{\"movementType\":\"INCOME\",\"categoryName\":\"Salario\"}", "groq", "llama-3.3", 12, 4));

        MovementClassification classification = service.classifyMovement(10L, "Me pagaron el sueldo", BigDecimal.valueOf(2500000));

        Assertions.assertEquals(CategoryType.INCOME, classification.type());
        Assertions.assertEquals(3L, classification.categoryId());
        Assertions.assertEquals("Salario", classification.categoryName());
        verify(aiUsageEventService).record(10L, "groq", AiUsageEventType.CATEGORIZE, 16, null);
    }

    @Test
    void classifyMovementShouldReturnExpenseWithMatchedCategoryForCleanJson() {
        Category transporte = buildCategory(2L, "Transporte", CategoryType.EXPENSE);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(11L, CategoryType.INCOME))
                .thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(11L, CategoryType.EXPENSE))
                .thenReturn(List.of(transporte));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(
                        "{\"movementType\":\"EXPENSE\",\"categoryName\":\"Transporte\"}", "groq", "llama-3.3", null, null));

        MovementClassification classification = service.classifyMovement(11L, "Uber al trabajo", BigDecimal.valueOf(15000));

        Assertions.assertEquals(CategoryType.EXPENSE, classification.type());
        Assertions.assertEquals(2L, classification.categoryId());
        Assertions.assertEquals("Transporte", classification.categoryName());
    }

    @Test
    void classifyMovementShouldFallBackToExpenseWithNullCategoryWhenJsonIsMalformed() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(12L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(12L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("esto no es JSON", "groq", "llama-3.3", 5, 3));

        MovementClassification classification = service.classifyMovement(12L, "Algo raro", BigDecimal.valueOf(1000));

        Assertions.assertEquals(CategoryType.EXPENSE, classification.type());
        Assertions.assertNull(classification.categoryId());
        Assertions.assertNull(classification.categoryName());
        verify(aiUsageEventService).record(12L, "groq", AiUsageEventType.CATEGORIZE, 8, null);
    }

    @Test
    void classifyMovementShouldFallBackToExpenseWhenMovementTypeIsUnrecognized() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(13L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(13L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(
                        "{\"movementType\":\"NOSEQUE\",\"categoryName\":null}", "groq", "llama-3.3", null, null));

        MovementClassification classification = service.classifyMovement(13L, "Algo ambiguo", BigDecimal.valueOf(1000));

        Assertions.assertEquals(CategoryType.EXPENSE, classification.type());
        Assertions.assertNull(classification.categoryId());
    }

    @Test
    void classifyMovementShouldNotRecordUsageWhenOrchestratorThrows() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(14L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(14L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.complete(anyList()))
                .thenThrow(new AiProviderNotConfiguredException(AiChatOrchestrator.GENERIC_MESSAGE));

        Assertions.assertThrows(AiProviderNotConfiguredException.class, () ->
                service.classifyMovement(14L, "Algo", BigDecimal.valueOf(1000))
        );
        verify(aiUsageEventService, never()).record(any(), any(), any(), anyInt(), any());
    }

    @Test
    void classifyMovementShouldHandleEmptyCategoryListsOnBothSides() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(15L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(15L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(
                        "{\"movementType\":\"INCOME\",\"categoryName\":null}", "groq", "llama-3.3", null, null));

        MovementClassification classification = service.classifyMovement(15L, "Venta de un mueble", BigDecimal.valueOf(50000));

        Assertions.assertEquals(CategoryType.INCOME, classification.type());
        Assertions.assertNull(classification.categoryId());
        Assertions.assertNull(classification.categoryName());
    }

    @Test
    void classifyMovementShouldParseJsonWrappedInMarkdownCodeFence() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(16L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(16L, CategoryType.EXPENSE)).thenReturn(List.of());
        String fenced = """
                ```json
                {"movementType":"EXPENSE","categoryName":null}
                ```
                """;
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult(fenced, "groq", "llama-3.3", null, null));

        MovementClassification classification = service.classifyMovement(16L, "Compra en el super", BigDecimal.valueOf(30000));

        Assertions.assertEquals(CategoryType.EXPENSE, classification.type());
    }

    private static Category buildCategory(Long id, String name, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        return category;
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
