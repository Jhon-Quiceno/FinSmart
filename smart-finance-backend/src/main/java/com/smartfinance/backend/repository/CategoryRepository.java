package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link Category}, always scoped by owner.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByUser_IdOrderByNameAsc(Long userId);

    List<Category> findAllByUser_IdAndTypeOrderByNameAsc(Long userId, CategoryType type);

    Optional<Category> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndNameIgnoreCaseAndType(Long userId, String name, CategoryType type);
}
