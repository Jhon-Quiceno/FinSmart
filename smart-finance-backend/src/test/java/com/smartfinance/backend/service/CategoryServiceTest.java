package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.category.CategoryRequest;
import com.smartfinance.backend.dto.category.CategoryResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.CategoryMapper;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

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
        CategoryRequest request = new CategoryRequest("Alimentación", CategoryType.EXPENSE);
        Category mappedCategory = new Category();
        Category savedCategory = new Category(
                10L, buildUser(1L), "Alimentación", CategoryType.EXPENSE, Instant.now(), Instant.now()
        );
        CategoryResponse response = new CategoryResponse(10L, "Alimentación", CategoryType.EXPENSE);

        when(categoryRepository.existsByUser_IdAndNameIgnoreCaseAndType(1L, "Alimentación", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(mappedCategory);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(categoryRepository.save(mappedCategory)).thenReturn(savedCategory);
        when(categoryMapper.toResponse(savedCategory)).thenReturn(response);

        CategoryResponse createdCategory = categoryService.createCategory(request);

        Assertions.assertEquals(10L, createdCategory.id());
        Assertions.assertEquals(1L, mappedCategory.getUser().getId());
    }

    @Test
    void createCategoryShouldThrowWhenNameAndTypeAlreadyExist() {
        setAuthenticatedUser(1L);
        CategoryRequest request = new CategoryRequest("Salud", CategoryType.EXPENSE);
        when(categoryRepository.existsByUser_IdAndNameIgnoreCaseAndType(1L, "Salud", CategoryType.EXPENSE))
                .thenReturn(true);

        Assertions.assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(request));
    }

    @Test
    void updateCategoryShouldThrowNotFoundWhenUserIsNotOwner() {
        setAuthenticatedUser(1L);
        CategoryRequest request = new CategoryRequest("Renta", CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(20L, request));
    }

    @Test
    void deleteCategoryShouldDeleteWhenOwnedByCurrentUser() {
        setAuthenticatedUser(3L);
        Category category = new Category();
        category.setId(55L);
        category.setName("Ahorro");
        category.setUser(buildUser(3L));
        when(categoryRepository.findByIdAndUser_Id(55L, 3L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(55L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategoryShouldThrowNotFoundWhenOwnedByAnotherUser() {
        setAuthenticatedUser(3L);
        when(categoryRepository.findByIdAndUser_Id(55L, 3L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory(55L));
    }

    @Test
    void getCategoriesShouldFilterByTypeWhenProvided() {
        setAuthenticatedUser(1L);
        Category category = new Category();
        category.setId(1L);
        category.setUser(buildUser(1L));
        category.setName("Salario");
        category.setType(CategoryType.INCOME);
        CategoryResponse response = new CategoryResponse(1L, "Salario", CategoryType.INCOME);

        when(categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(1L, CategoryType.INCOME))
                .thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        List<CategoryResponse> result = categoryService.getCategories(CategoryType.INCOME);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Salario", result.get(0).name());
    }

    @Test
    void getCategoriesShouldReturnAllForUserWhenTypeIsNull() {
        setAuthenticatedUser(1L);
        when(categoryRepository.findAllByUser_IdOrderByNameAsc(1L)).thenReturn(List.of());

        List<CategoryResponse> result = categoryService.getCategories(null);

        Assertions.assertTrue(result.isEmpty());
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
