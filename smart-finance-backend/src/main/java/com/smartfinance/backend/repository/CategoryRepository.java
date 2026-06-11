package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            SELECT c
            FROM Category c
            LEFT JOIN c.user u
            WHERE c.isSystem = true OR u.id = :userId
            ORDER BY c.name ASC
            """)
    List<Category> findAllAccessibleByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT c
            FROM Category c
            LEFT JOIN c.user u
            WHERE (c.isSystem = true OR u.id = :userId)
              AND c.type = :type
            ORDER BY c.name ASC
            """)
    List<Category> findAllAccessibleByUserIdAndType(@Param("userId") Long userId, @Param("type") CategoryType type);

    @Query("""
            SELECT c
            FROM Category c
            LEFT JOIN c.user u
            WHERE c.id = :categoryId
              AND (c.isSystem = true OR u.id = :userId)
            """)
    Optional<Category> findAccessibleByIdAndUserId(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    boolean existsByUser_IdAndNameIgnoreCase(Long userId, String name);
}
