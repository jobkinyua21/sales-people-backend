package com.possystem.kitchen.kot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.kitchen.recipe.PrepStation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KotResponse {

    private UUID id;
    private UUID shopId;
    private String kotNumber;
    private UUID orderId;
    private String orderNumber;
    private String tableNumber;
    private KotStatus status;
    private Integer priority;
    private String specialInstructions;
    private UUID sentBy;
    private String sentByName;
    private UUID acceptedBy;
    private String acceptedByName;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private Boolean ingredientsDeducted;
    private LocalDateTime createdAt;
    private List<KotItemResponse> items;

    // Summary stats
    private Integer totalItems;
    private Integer pendingItems;
    private Integer preparingItems;
    private Integer readyItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KotItemResponse {
        private UUID id;
        private UUID orderItemId;
        private UUID variantId;
        private String productName;
        private String variantName;
        private BigDecimal quantity;
        private KotItemStatus status;
        private PrepStation prepStation;
        private String specialInstructions;
        private UUID recipeId;
        private UUID preparedBy;
        private String preparedByName;
        private LocalDateTime startedAt;
        private LocalDateTime readyAt;
        private LocalDateTime servedAt;
        private LocalDateTime cancelledAt;
        private String cancelReason;
    }
}
