package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.category.CategoryRequest;
import com.smartfinance.backend.dto.category.CategoryResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.CategoryMapper;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(CategoryType type) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Category> categories = type == null
                ? categoryRepository.findAllAccessibleByUserId(userId)
                : categoryRepository.findAllAccessibleByUserIdAndType(userId, type);

        return categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String normalizedName = request.name().trim();

        if (categoryRepository.existsByUser_IdAndNameIgnoreCase(userId, normalizedName)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }

        Category category = categoryMapper.toEntity(request);
        category.setName(normalizedName);
        category.setSystem(false);
        category.setUser(buildUserReference(userId));

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        validateOwnershipForMutation(category, userId);
        validateDuplicateName(category, userId, request.name().trim());

        categoryMapper.updateEntityFromRequest(request, category);
        category.setName(request.name().trim());
        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        validateOwnershipForMutation(category, userId);
        categoryRepository.delete(category);
    }

    private void validateOwnershipForMutation(Category category, Long userId) {
        if (category.isSystem()) {
            throw new AccessDeniedException("No se pueden modificar categorías del sistema");
        }

        Long ownerId = category.getUser() == null ? null : category.getUser().getId();
        if (!userId.equals(ownerId)) {
            throw new AccessDeniedException("No tienes permisos sobre esta categoría");
        }
    }

    private void validateDuplicateName(Category category, Long userId, String newName) {
        if (category.getName().equalsIgnoreCase(newName)) {
            return;
        }

        if (categoryRepository.existsByUser_IdAndNameIgnoreCase(userId, newName)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
    }

    private User buildUserReference(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
