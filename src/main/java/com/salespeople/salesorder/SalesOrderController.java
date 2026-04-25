package com.salespeople.salesorder;

import com.salespeople.common.ApiResponse;
import com.salespeople.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> create(@Valid @RequestBody SalesOrderRequest request) {
        SalesOrderResponse response = salesOrderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Sales order created successfully"));
    }

    @PostMapping("/fetch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ListResponse<SalesOrderResponse>> fetch(@RequestBody SalesOrderFetchRequest request) {
        return ResponseEntity.ok(salesOrderService.fetch(request));
    }

    @PostMapping("/cancel/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> cancel(@PathVariable Long id) {
        SalesOrderResponse response = salesOrderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order cancelled"));
    }
}
