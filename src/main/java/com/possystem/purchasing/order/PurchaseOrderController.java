package com.possystem.purchasing.order;

import com.possystem.common.ApiResponse;
import com.possystem.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> save(
            @Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderResponse response = purchaseOrderService.save(request);
        String message = request.getId() != null ? "Purchase order updated" : "Purchase order created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    public ResponseEntity<ListResponse<PurchaseOrderResponse>> fetch(
            @RequestBody PurchaseOrderFetchRequest request) {
        ListResponse<PurchaseOrderResponse> response = purchaseOrderService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> submit(
            @Valid @RequestBody PurchaseOrderActionRequest request) {
        PurchaseOrderResponse response = purchaseOrderService.submitOrder(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase order submitted"));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancel(
            @Valid @RequestBody PurchaseOrderActionRequest request) {
        PurchaseOrderResponse response = purchaseOrderService.cancelOrder(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase order cancelled"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        purchaseOrderService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Purchase order deleted"));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        purchaseOrderService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, "Purchase orders deleted"));
    }
}
