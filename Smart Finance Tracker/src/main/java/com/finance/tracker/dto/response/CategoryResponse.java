package com.finance.tracker.dto.response;

import com.finance.tracker.entity.Category;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    @JsonProperty("type")
    private Category.CategoryType categoryType;
    private Boolean isDefault;
    private Long userId; 
    private String description;
    private String icon;
    private String color;
    private String status;
    private Boolean createdByAdmin;
    private Long usedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
