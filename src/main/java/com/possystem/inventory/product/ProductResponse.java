package com.possystem.inventory;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private UUID id;
    private UUID shopId;
    private UUID categoryId;
    private String categoryName;
    private String productCode;
    private String productName;
    private String description;
    private String imageUrl;
    private ProductType productType;
    private ProductStatus status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ProductVariantResponse> variants;
}
