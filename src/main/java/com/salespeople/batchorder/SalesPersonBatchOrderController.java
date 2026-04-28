package com.salespeople.batchorder;

import com.salespeople.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batch-orders")
@RequiredArgsConstructor
public class SalesPersonBatchOrderController {

    private final SalesPersonBatchOrderService batchOrderService;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SalesPersonBatchOrderResponse>>> create(
            @Valid @RequestBody SalesPersonBatchOrderRequest request) {
        List<SalesPersonBatchOrderResponse> response = batchOrderService.createBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Batch created successfully"));
    }

    @GetMapping("/my-batches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SalesPersonBatchOrderResponse>>> myBatches() {
        return ResponseEntity.ok(ApiResponse.success(batchOrderService.fetchMyBatches(), "Batches fetched"));
    }

    @GetMapping("/{batchRef}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SalesPersonBatchOrderResponse>>> getByBatchRef(
            @PathVariable String batchRef) {
        return ResponseEntity.ok(ApiResponse.success(batchOrderService.fetchByBatchRef(batchRef), "Batch fetched"));
    }

    @GetMapping("/sales-person/{salesPersonNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SalesPersonBatchOrderResponse>>> getBySalesPerson(
            @PathVariable Integer salesPersonNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                batchOrderService.fetchBySalesPerson(salesPersonNumber), "Batches fetched"));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SalesPersonBatchOrderResponse>>> getByStatus(
            @PathVariable SalesPersonBatchOrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(batchOrderService.fetchByStatus(status), "Batches fetched"));
    }

    @PostMapping("/{batchRef}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SalesPersonBatchOrderResponse>>> approve(
            @PathVariable String batchRef) {
        return ResponseEntity.ok(ApiResponse.success(
                batchOrderService.approveBatch(batchRef), "Batch approved successfully"));
    }

    @PostMapping("/{batchRef}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SalesPersonBatchOrderResponse>>> reject(
            @PathVariable String batchRef,
            @RequestBody SalesPersonBatchOrderReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                batchOrderService.rejectBatch(batchRef, request), "Batch rejected"));
    }
}
