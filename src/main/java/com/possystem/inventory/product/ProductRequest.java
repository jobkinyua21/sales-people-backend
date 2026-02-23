package com.possystem.inventory;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    private UUID id;

    @NotBlank(message = "Product name is required")
    private String productName;

    private UUID categoryId;
    private String description;
    private String imageUrl;
    private ProductType productType;
    private ProductStatus status;

    // For SIMPLE products: inline default variant data
    private ProductVariantRequest defaultVariant;

    // For VARIABLE products: list of variants (create/update/sync)
    // On create: all variants created
    // On update: id present = update, id absent = create new, existing not in list = soft delete
    private List<ProductVariantRequest> variants;
}
