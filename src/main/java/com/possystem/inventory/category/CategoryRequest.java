package com.possystem.inventory;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    private UUID id;

    @NotBlank(message = "Category name is required")
    private String categoryName;

    private String description;

    private String imageUrl;

    private Integer sortOrder;

    private CategoryStatus status;
}
