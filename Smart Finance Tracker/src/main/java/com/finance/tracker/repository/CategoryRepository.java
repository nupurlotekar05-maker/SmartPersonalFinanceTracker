package com.finance.tracker.repository;

import com.finance.tracker.dto.response.CategoryResponse;
import com.finance.tracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdIsNullOrUserId(Long userId);

    List<Category> findByUserIdIsNull();

    List<Category> findByUserId(Long userId);

    List<Category> findByUserIdIsNullAndCategoryTypeAndStatus(Category.CategoryType type, Category.CategoryStatus status);

    List<Category> findByUserIdAndCategoryTypeAndStatus(Long userId, Category.CategoryType type, Category.CategoryStatus status);

    void deleteByUserId(Long userId);

    @Query("SELECT c FROM Category c WHERE c.user IS NULL AND c.isDefault = true")
    List<Category> findAllDefaultCategories();

    Optional<Category> findByNameAndUserIdIsNull(String name);

    Optional<Category> findByIdAndUserIdIsNull(Long id);

    boolean existsByNameAndUserIdIsNull(String name);

    Optional<Category> findByNameIgnoreCaseAndUserId(String name, Long userId);

    Optional<Category> findByNameIgnoreCaseAndUserIdIsNull(String name);

    @Query("SELECT c FROM Category c WHERE (c.user IS NULL OR c.user.id = :userId)")
    List<Category> findByUserIdOrDefault(@Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.user.id = :userId")
    Optional<Category> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE (c.user IS NULL OR c.user.id = :userId) AND c.categoryType IN :types")
    List<Category> findByUserIdOrDefaultAndCategoryTypeIn(@Param("userId") Long userId,
            @Param("types") Collection<Category.CategoryType> types);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.user.id = :userId OR c.user IS NULL)")
    Optional<Category> findByIdAndUserIdOrDefault(@Param("id") Long id, @Param("userId") Long userId);
}
