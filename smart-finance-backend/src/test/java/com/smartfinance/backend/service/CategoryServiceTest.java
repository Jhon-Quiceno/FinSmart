package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.category.CategoryRequest;
import com.smartfinance.backend.dto.category.CategoryResponse;
import com.smartfinance.backend.mapper.CategoryMapper;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCategoryShouldSaveCategoryForCurrentUser() {
        setAuthenticatedUser(1L);
        CategoryRequest request = new CategoryRequest("Alimentación", CategoryType.EXPENSE, "utensils", "#F59E0B");
        Category mappedCategory = new Category();
        Category savedCategory = new Category(
                10L,
                buildUser(1L),
                "Alimentación",
                CategoryType.EXPENSE,
                "utensils",
                "#F59E0B",
                false,
                Instant.now(),
                Instant.now()
        );
        CategoryResponse response = new CategoryResponse(
                10L,
                "Alimentación",
                CategoryType.EXPENSE,
                "utensils",
                "#F59E0B",
                false,
                savedCategory.getCreatedAt(),
                savedCategory.getUpdatedAt()
        );

        when(categoryRepository.existsByUser_IdAndNameIgnoreCase(1L, "Alimentación")).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(mappedCategory);
        when(categoryRepository.save(mappedCategory)).thenReturn(savedCategory);
        when(categoryMapper.toResponse(savedCategory)).thenReturn(response);

        CategoryResponse createdCategory = categoryService.createCategory(request);

        Assertions.assertEquals(10L, createdCategory.id());
        Assertions.assertFalse(mappedCategory.isSystem());
        Assertions.assertEquals(1L, mappedCategory.getUser().getId());
    }

    @Test
    void createCategoryShouldThrowWhenNameAlreadyExists() {
        setAuthenticatedUser(1L);
        CategoryRequest request = new CategoryRequest("Salud", CategoryType.EXPENSE, null, "#EF4444");
        when(categoryRepository.existsByUser_IdAndNameIgnoreCase(1L, "Salud")).thenReturn(true);

        Assertions.assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(request));
    }

    @Test
    void updateCategoryShouldThrowWhenUserIsNotOwner() {
        setAuthenticatedUser(1L);
        CategoryRequest request = new CategoryRequest("Renta", CategoryType.EXPENSE, null, "#6366F1");
        Category category = new Category();
        category.setId(20L);
        category.setName("Renta");
        category.setUser(buildUser(2L));
        category.setSystem(false);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));

        Assertions.assertThrows(AccessDeniedException.class, () -> categoryService.updateCategory(20L, request));
    }

    @Test
    void deleteCategoryShouldDeleteWhenOwnedByCurrentUser() {
        setAuthenticatedUser(3L);
        Category category = new Category();
        category.setId(55L);
        category.setName("Ahorro");
        category.setUser(buildUser(3L));
        category.setSystem(false);
        when(categoryRepository.findById(55L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(55L);

        verify(categoryRepository).delete(category);
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }

    private User buildUser(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
