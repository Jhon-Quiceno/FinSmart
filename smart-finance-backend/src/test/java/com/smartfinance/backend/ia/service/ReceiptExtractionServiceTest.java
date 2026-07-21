package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.gastos.model.entity.Category;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.gastos.repository.CategoryRepository;
import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.ia.model.dto.ReceiptExtraction;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.ia.service.ai.ChatCompletionResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptExtractionServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AiChatOrchestrator aiChatOrchestrator;

    @Mock
    private AiUsageEventService aiUsageEventService;

    private ReceiptExtractionService service;

    @BeforeEach
    void setUp() {
        // JsonMapper es la implementación concreta de ObjectMapper en Jackson 3 (tools.jackson),
        // el mismo tipo que Spring Boot 4 auto-configura e inyecta en el servicio real (ver
        // AiCategorizationServiceTest, mismo patrón para servicios que parsean JSON de IA).
        service = new ReceiptExtractionService(categoryRepository, aiChatOrchestrator, aiUsageEventService, JsonMapper.builder().build());
    }

    @Test
    void extractFromImageShouldReturnExpenseWithMatchedCategoryForCleanJson() {
        Category transporte = buildCategory(2L, "Transporte", CategoryType.EXPENSE);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(1L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(1L, CategoryType.EXPENSE)).thenReturn(List.of(transporte));
        when(aiChatOrchestrator.completeVision(anyList())).thenReturn(new ChatCompletionResult(
                "{\"isReceipt\":true,\"description\":\"TESCO\",\"amount\":6.71,\"movementType\":\"EXPENSE\",\"categoryName\":\"Transporte\"}",
                "nvidia", "nvidia/nemotron-nano-12b-v2-vl", 100, 20
        ));

        ReceiptExtraction extraction = service.extractFromImage(1L, "data:image/jpeg;base64,abc");

        Assertions.assertTrue(extraction.isReceipt());
        Assertions.assertEquals("TESCO", extraction.description());
        Assertions.assertEquals(0, BigDecimal.valueOf(6.71).compareTo(extraction.amount()));
        Assertions.assertEquals(CategoryType.EXPENSE, extraction.movementType());
        Assertions.assertEquals(2L, extraction.categoryId());
        Assertions.assertEquals("Transporte", extraction.categoryName());
        verify(aiUsageEventService).record(1L, "nvidia", AiUsageEventType.CATEGORIZE, 120, null);
    }

    @Test
    void extractFromImageShouldReturnIncomeWithMatchedCategoryForCleanJson() {
        Category reembolso = buildCategory(5L, "Reembolso", CategoryType.INCOME);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(2L, CategoryType.INCOME)).thenReturn(List.of(reembolso));
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(2L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.completeVision(anyList())).thenReturn(new ChatCompletionResult(
                "{\"isReceipt\":true,\"description\":\"Nota de crédito\",\"amount\":15000,\"movementType\":\"INCOME\",\"categoryName\":\"Reembolso\"}",
                "nvidia", "nvidia/nemotron-nano-12b-v2-vl", null, null
        ));

        ReceiptExtraction extraction = service.extractFromImage(2L, "data:image/jpeg;base64,abc");

        Assertions.assertEquals(CategoryType.INCOME, extraction.movementType());
        Assertions.assertEquals(5L, extraction.categoryId());
    }

    @Test
    void extractFromImageShouldReturnNotAReceiptWhenModelReportsIsReceiptFalse() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(3L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(3L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.completeVision(anyList())).thenReturn(new ChatCompletionResult(
                "{\"isReceipt\":false,\"description\":null,\"amount\":null,\"movementType\":null,\"categoryName\":null}",
                "nvidia", "nvidia/nemotron-nano-12b-v2-vl", null, null
        ));

        ReceiptExtraction extraction = service.extractFromImage(3L, "data:image/jpeg;base64,cat-photo");

        assertThat(extraction).isEqualTo(ReceiptExtraction.notAReceipt());
    }

    @Test
    void extractFromImageShouldDegradeToNotAReceiptWhenJsonIsMalformed() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(4L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(4L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.completeVision(anyList()))
                .thenReturn(new ChatCompletionResult("esto no es JSON", "nvidia", "nvidia/nemotron-nano-12b-v2-vl", 5, 3));

        ReceiptExtraction extraction = service.extractFromImage(4L, "data:image/jpeg;base64,abc");

        assertThat(extraction).isEqualTo(ReceiptExtraction.notAReceipt());
        verify(aiUsageEventService).record(4L, "nvidia", AiUsageEventType.CATEGORIZE, 8, null);
    }

    @Test
    void extractFromImageShouldDegradeToNotAReceiptWhenAmountIsMissing() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(6L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(6L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.completeVision(anyList())).thenReturn(new ChatCompletionResult(
                "{\"isReceipt\":true,\"description\":\"Comercio\",\"amount\":null,\"movementType\":\"EXPENSE\",\"categoryName\":null}",
                "nvidia", "nvidia/nemotron-nano-12b-v2-vl", null, null
        ));

        ReceiptExtraction extraction = service.extractFromImage(6L, "data:image/jpeg;base64,abc");

        assertThat(extraction).isEqualTo(ReceiptExtraction.notAReceipt());
    }

    @Test
    void extractFromImageShouldDegradeToNotAReceiptWhenAmountIsZeroOrNegative() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(7L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(7L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.completeVision(anyList())).thenReturn(new ChatCompletionResult(
                "{\"isReceipt\":true,\"description\":\"Comercio\",\"amount\":0,\"movementType\":\"EXPENSE\",\"categoryName\":null}",
                "nvidia", "nvidia/nemotron-nano-12b-v2-vl", null, null
        ));

        ReceiptExtraction extraction = service.extractFromImage(7L, "data:image/jpeg;base64,abc");

        assertThat(extraction).isEqualTo(ReceiptExtraction.notAReceipt());
    }

    @Test
    void extractFromImageShouldDegradeToNotAReceiptWhenDescriptionIsBlank() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(8L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(8L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.completeVision(anyList())).thenReturn(new ChatCompletionResult(
                "{\"isReceipt\":true,\"description\":\"   \",\"amount\":5000,\"movementType\":\"EXPENSE\",\"categoryName\":null}",
                "nvidia", "nvidia/nemotron-nano-12b-v2-vl", null, null
        ));

        ReceiptExtraction extraction = service.extractFromImage(8L, "data:image/jpeg;base64,abc");

        assertThat(extraction).isEqualTo(ReceiptExtraction.notAReceipt());
    }

    @Test
    void extractFromImageShouldDefaultToExpenseWhenMovementTypeIsUnrecognized() {
        Category comida = buildCategory(1L, "Comida", CategoryType.EXPENSE);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(9L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(9L, CategoryType.EXPENSE)).thenReturn(List.of(comida));
        when(aiChatOrchestrator.completeVision(anyList())).thenReturn(new ChatCompletionResult(
                "{\"isReceipt\":true,\"description\":\"Restaurante\",\"amount\":5000,\"movementType\":\"NOSEQUE\",\"categoryName\":\"Comida\"}",
                "nvidia", "nvidia/nemotron-nano-12b-v2-vl", null, null
        ));

        ReceiptExtraction extraction = service.extractFromImage(9L, "data:image/jpeg;base64,abc");

        Assertions.assertEquals(CategoryType.EXPENSE, extraction.movementType());
        Assertions.assertEquals(1L, extraction.categoryId());
    }

    @Test
    void extractFromImageShouldReturnNullCategoryWhenNoCategoryNameIsSuggested() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(10L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(10L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.completeVision(anyList())).thenReturn(new ChatCompletionResult(
                "{\"isReceipt\":true,\"description\":\"Comercio\",\"amount\":5000,\"movementType\":\"EXPENSE\",\"categoryName\":null}",
                "nvidia", "nvidia/nemotron-nano-12b-v2-vl", null, null
        ));

        ReceiptExtraction extraction = service.extractFromImage(10L, "data:image/jpeg;base64,abc");

        Assertions.assertNull(extraction.categoryId());
        Assertions.assertNull(extraction.categoryName());
    }

    @Test
    void extractFromImageShouldParseJsonWrappedInMarkdownCodeFence() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(11L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(11L, CategoryType.EXPENSE)).thenReturn(List.of());
        String fenced = """
                ```json
                {"isReceipt":true,"description":"Comercio","amount":5000,"movementType":"EXPENSE","categoryName":null}
                ```
                """;
        when(aiChatOrchestrator.completeVision(anyList()))
                .thenReturn(new ChatCompletionResult(fenced, "nvidia", "nvidia/nemotron-nano-12b-v2-vl", null, null));

        ReceiptExtraction extraction = service.extractFromImage(11L, "data:image/jpeg;base64,abc");

        Assertions.assertTrue(extraction.isReceipt());
        Assertions.assertEquals("Comercio", extraction.description());
    }

    @Test
    void extractFromImageShouldPropagateProviderFailureWithoutRecordingUsage() {
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(12L, CategoryType.INCOME)).thenReturn(List.of());
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(12L, CategoryType.EXPENSE)).thenReturn(List.of());
        when(aiChatOrchestrator.completeVision(anyList()))
                .thenThrow(new AiProviderNotConfiguredException(AiChatOrchestrator.GENERIC_MESSAGE));

        Assertions.assertThrows(AiProviderNotConfiguredException.class, () -> service.extractFromImage(12L, "data:image/jpeg;base64,abc"));
        verify(aiUsageEventService, never()).record(any(), any(), any(), anyInt(), any());
    }

    private static Category buildCategory(Long id, String name, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        return category;
    }
}
