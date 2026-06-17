package com.finance.tracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoryRequest {

    @NotBlank
    private String name;

    @NotNull
    private String type; // INCOME or EXPENSE

    private String description;
    private String icon;
    private String color;
    private String status; // ACTIVE or INACTIVE
    private Boolean isDefault = true;
}
