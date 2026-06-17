package com.finance.tracker.dto.request;

import com.finance.tracker.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    @NotNull(message = "Type is required (INCOME, EXPENSE, or BOTH)")
    private Category.CategoryType categoryType;

    private String description;
    private String icon;
    private String color;
}
