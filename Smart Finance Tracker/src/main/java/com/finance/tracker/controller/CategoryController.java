package com.finance.tracker.controller;

import com.finance.tracker.dto.request.CategoryRequest;
import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.dto.response.CategoryResponse;
import com.finance.tracker.entity.Category;
import com.finance.tracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category created", categoryService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllForCurrentUser()));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllDetailed() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllForCurrentUser()));
    }

    @GetMapping("/expense")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getExpenseCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getExpenseCategories()));
    }

    @GetMapping("/income")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getIncomeCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getIncomeCategories()));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoriesByType(@PathVariable Category.CategoryType type) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoriesByType(type)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}
