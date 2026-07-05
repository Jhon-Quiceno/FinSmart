package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.ai.CategorizeRequest;
import com.smartfinance.backend.dto.ai.CategorizeResponse;
import com.smartfinance.backend.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.service.ai.ChatCompletionResult;
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
import java.util.List;

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

    @InjectMocks
    private AiCategorizationService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void categorizeShouldReturnNullMatchWhenUserHasNoExpenseCategories() {
        setAuthenticatedUser(1L);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(1L, CategoryType.EXPENSE)).thenReturn(List.of());

        CategorizeResponse response = service.categorize(new CategorizeRequest("Almuerzo en restaurante", null));

        Assertions.assertNull(response.categoryId());
        Assertions.assertNull(response.categoryName());
        verify(aiChatOrchestrator, never()).complete(anyList());
    }

    @Test
    void categorizeShouldThrowWhenNoProviderIsConfigured() {
        setAuthenticatedUser(2L);
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(2L, CategoryType.EXPENSE))
                .thenReturn(List.of(buildCategory(1L, "Comida")));
        when(aiChatOrchestrator.complete(anyList()))
                .thenThrow(new AiProviderNotConfiguredException(AiChatOrchestrator.GENERIC_MESSAGE));

        Assertions.assertThrows(AiProviderNotConfiguredException.class, () ->
                service.categorize(new CategorizeRequest("Almuerzo", null))
        );
    }

    @Test
    void categorizeShouldReturnExactMatchWhenModelRespondsWithExactCategoryName() {
        setAuthenticatedUser(3L);
        Category comida = buildCategory(1L, "Comida");
        Category transporte = buildCategory(2L, "Transporte");
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(3L, CategoryType.EXPENSE))
                .thenReturn(List.of(comida, transporte));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("Comida", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Almuerzo en restaurante", BigDecimal.valueOf(25000)));

        Assertions.assertEquals(1L, response.categoryId());
        Assertions.assertEquals("Comida", response.categoryName());
    }

    @Test
    void categorizeShouldTolerateExtraChatterAroundTheCategoryName() {
        setAuthenticatedUser(4L);
        Category transporte = buildCategory(2L, "Transporte");
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(4L, CategoryType.EXPENSE))
                .thenReturn(List.of(transporte));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("La categoría es Transporte.", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Uber al trabajo", null));

        Assertions.assertEquals(2L, response.categoryId());
        Assertions.assertEquals("Transporte", response.categoryName());
    }

    @Test
    void categorizeShouldReturnNullMatchWhenModelRespondsWithNoMatchToken() {
        setAuthenticatedUser(5L);
        Category comida = buildCategory(1L, "Comida");
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(5L, CategoryType.EXPENSE))
                .thenReturn(List.of(comida));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("NINGUNA", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Pago de arriendo", null));

        Assertions.assertNull(response.categoryId());
        Assertions.assertNull(response.categoryName());
    }

    @Test
    void categorizeShouldMatchCaseInsensitively() {
        setAuthenticatedUser(6L);
        Category comida = buildCategory(1L, "Comida");
        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(6L, CategoryType.EXPENSE))
                .thenReturn(List.of(comida));
        when(aiChatOrchestrator.complete(anyList()))
                .thenReturn(new ChatCompletionResult("comida", "groq", "llama-3.3", null, null));

        CategorizeResponse response = service.categorize(new CategorizeRequest("Mercado", null));

        Assertions.assertEquals(1L, response.categoryId());
    }

    private static Category buildCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(CategoryType.EXPENSE);
        return category;
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
