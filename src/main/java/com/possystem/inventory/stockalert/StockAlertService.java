package com.possystem.inventory.stockalert;

import com.possystem.inventory.InventoryStock;
import com.possystem.inventory.InventoryStockRepository;
import com.possystem.inventory.ProductVariant;
import com.possystem.inventory.ProductVariantRepository;
import com.possystem.inventory.Product;
import com.possystem.inventory.ProductRepository;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockAlertService {

    private final StockAlertRepository stockAlertRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    // ==================== ALERT GENERATION ====================

    /**
     * Called after stock is deducted (e.g., after a sale is completed).
     * Checks if the variant's stock has fallen to low or out-of-stock levels
     * and creates an alert if one doesn't already exist.
     */
    @Transactional
    public void checkAndCreateAlert(UUID shopId, UUID variantId) {
        InventoryStock stock = inventoryStockRepository
                .findByVariantIdAndShopIdAndIsActiveTrue(variantId, shopId)
                .orElse(null);

        if (stock == null) return;

        ProductVariant variant = productVariantRepository
                .findByIdAndShopIdAndIsActiveTrue(variantId, shopId)
                .orElse(null);

        if (variant == null) return;

        Product product = productRepository
                .findByIdAndShopIdAndIsActiveTrue(variant.getProductId(), shopId)
                .orElse(null);

        if (product == null) return;

        BigDecimal qty = stock.getCurrentQuantity();

        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            // Out of stock — upgrade existing LOW_STOCK alert or create new
            resolveExistingAlert(variantId, shopId, StockAlertType.LOW_STOCK);
            createAlertIfNotExists(shopId, stock, variant, product, StockAlertType.OUT_OF_STOCK);
        } else if (stock.getReorderLevel() != null && qty.compareTo(stock.getReorderLevel()) <= 0) {
            // Low stock
            createAlertIfNotExists(shopId, stock, variant, product, StockAlertType.LOW_STOCK);
        }
    }

    /**
     * Called when stock is restocked (e.g., GRN received, manual stock update).
     * Auto-resolves alerts if stock is back above reorder level.
     */
    @Transactional
    public void checkAndResolveAlert(UUID shopId, UUID variantId) {
        InventoryStock stock = inventoryStockRepository
                .findByVariantIdAndShopIdAndIsActiveTrue(variantId, shopId)
                .orElse(null);

        if (stock == null) return;

        BigDecimal qty = stock.getCurrentQuantity();

        // If stock is back above reorder level, resolve all active alerts
        if (qty.compareTo(BigDecimal.ZERO) > 0) {
            resolveExistingAlert(variantId, shopId, StockAlertType.OUT_OF_STOCK);
        }

        if (stock.getReorderLevel() != null && qty.compareTo(stock.getReorderLevel()) > 0) {
            resolveExistingAlert(variantId, shopId, StockAlertType.LOW_STOCK);
        }
    }

    // ==================== QUERIES ====================

    public List<StockAlertResponse> getActiveAlerts() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        return stockAlertRepository.findAllByShopIdAndStatusOrderByCreatedAtDesc(shopId, StockAlertStatus.ACTIVE)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    public List<StockAlertResponse> getAllAlerts() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        return stockAlertRepository.findAllByShopIdOrderByCreatedAtDesc(shopId)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    public long getActiveAlertCount() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        return stockAlertRepository.countActiveByShopId(shopId);
    }

    // ==================== ACTIONS ====================

    @Transactional
    public StockAlertResponse acknowledgeAlert(UUID alertId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        StockAlert alert = stockAlertRepository.findByIdAndShopId(alertId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));

        if (alert.getStatus() != StockAlertStatus.ACTIVE) {
            throw new IllegalArgumentException("Alert is not active");
        }

        alert.setStatus(StockAlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedAt(LocalDateTime.now());

        StockAlert saved = stockAlertRepository.save(alert);
        return buildResponse(saved);
    }

    @Transactional
    public StockAlertResponse resolveAlert(UUID alertId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        StockAlert alert = stockAlertRepository.findByIdAndShopId(alertId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));

        if (alert.getStatus() == StockAlertStatus.RESOLVED) {
            throw new IllegalArgumentException("Alert is already resolved");
        }

        alert.setStatus(StockAlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());

        StockAlert saved = stockAlertRepository.save(alert);
        return buildResponse(saved);
    }

    // ==================== INTERNAL ====================

    private void createAlertIfNotExists(UUID shopId, InventoryStock stock, ProductVariant variant, Product product, StockAlertType alertType) {
        boolean exists = stockAlertRepository.existsByVariantIdAndShopIdAndAlertTypeAndStatus(
                variant.getId(), shopId, alertType, StockAlertStatus.ACTIVE);

        if (exists) return;

        StockAlert alert = StockAlert.builder()
                .shopId(shopId)
                .variantId(variant.getId())
                .productName(product.getProductName())
                .variantName(variant.getVariantName())
                .sku(variant.getSku())
                .alertType(alertType)
                .status(StockAlertStatus.ACTIVE)
                .currentQuantity(stock.getCurrentQuantity())
                .reorderLevel(stock.getReorderLevel())
                .build();

        stockAlertRepository.save(alert);
        log.info("Stock alert created: {} for {} ({}) - qty: {}, reorder: {}",
                alertType, product.getProductName(), variant.getSku(),
                stock.getCurrentQuantity(), stock.getReorderLevel());
    }

    private void resolveExistingAlert(UUID variantId, UUID shopId, StockAlertType alertType) {
        stockAlertRepository.findByVariantIdAndShopIdAndAlertTypeAndStatus(
                        variantId, shopId, alertType, StockAlertStatus.ACTIVE)
                .ifPresent(alert -> {
                    alert.setStatus(StockAlertStatus.RESOLVED);
                    alert.setResolvedAt(LocalDateTime.now());
                    stockAlertRepository.save(alert);
                });
    }

    // ==================== RESPONSE ====================

    private StockAlertResponse buildResponse(StockAlert alert) {
        return StockAlertResponse.builder()
                .id(alert.getId())
                .shopId(alert.getShopId())
                .variantId(alert.getVariantId())
                .productName(alert.getProductName())
                .variantName(alert.getVariantName())
                .sku(alert.getSku())
                .alertType(alert.getAlertType())
                .status(alert.getStatus())
                .currentQuantity(alert.getCurrentQuantity())
                .reorderLevel(alert.getReorderLevel())
                .acknowledgedBy(alert.getAcknowledgedBy())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .resolvedAt(alert.getResolvedAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
