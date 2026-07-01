package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.category.CategoryRequest;
import com.smartfinance.backend.dto.category.CategoryResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.CategoryMapper;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.repository.CategoryRepository;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for managing the current user's {@link Category} records.
 *
 * <p>Every operation resolves the caller via {@link SecurityUtils#getCurrentUserId()} and
 * scopes reads/writes strictly to that user. Mutations on a category owned by another user
 * raise {@link ResourceNotFoundException} (HTTP 404) rather than a 403, so category
 * existence is never leaked to a non-owner.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            CategoryMapper categoryMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(CategoryType type) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Category> categories = type == null
                ? categoryRepository.findAllByUser_IdOrderByNameAsc(userId)
                : categoryRepository.findAllByUser_IdAndTypeOrderByNameAsc(userId, type);

        return categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String normalizedName = request.name().trim();
        validateNameNotDuplicated(userId, normalizedName, request.type(), null);

        Category category = categoryMapper.toEntity(request);
        category.setName(normalizedName);
        category.setUser(userRepository.getReferenceById(userId));

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Category category = findOwnedCategory(categoryId, userId);

        String normalizedName = request.name().trim();
        validateNameNotDuplicated(userId, normalizedName, request.type(), category);

        categoryMapper.updateEntityFromRequest(request, category);
        category.setName(normalizedName);

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Category category = findOwnedCategory(categoryId, userId);
        categoryRepository.delete(category);
    }

    private Category findOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }

    private void validateNameNotDuplicated(Long userId, String name, CategoryType type, Category categoryBeingUpdated) {
        boolean unchanged = categoryBeingUpdated != null
                && categoryBeingUpdated.getName().equalsIgnoreCase(name)
                && categoryBeingUpdated.getType() == type;
        if (unchanged) {
            return;
        }

        if (categoryRepository.existsByUser_IdAndNameIgnoreCaseAndType(userId, name, type)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre y tipo");
        }
    }
}
