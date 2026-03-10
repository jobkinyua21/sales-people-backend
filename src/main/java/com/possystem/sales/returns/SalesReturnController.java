package com.possystem.sales.returns;

import com.possystem.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales-returns")
@RequiredArgsConstructor
public class SalesReturnController {

    private final SalesReturnService salesReturnService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_RETURNS_CREATE')")
    public ResponseEntity<ApiResponse<SalesReturnResponse>> create(@Valid @RequestBody SalesReturnRequest request) {
        SalesReturnResponse response = salesReturnService.createReturn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "Return request created. Awaiting manager approval."));
    }

    @PostMapping("/approve/{returnId}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_RETURNS_APPROVE')")
    public ResponseEntity<ApiResponse<SalesReturnResponse>> approve(@PathVariable UUID returnId) {
        SalesReturnResponse response = salesReturnService.approveReturn(returnId);
        return ResponseEntity.ok(ApiResponse.success(response, "Return approved and processed"));
    }

    @PostMapping("/reject/{returnId}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_RETURNS_APPROVE')")
    public ResponseEntity<ApiResponse<SalesReturnResponse>> reject(@PathVariable UUID returnId, @Valid @RequestBody RejectReturnRequest request) {
        SalesReturnResponse response = salesReturnService.rejectReturn(returnId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Return rejected"));
    }

    @PostMapping("/all")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_RETURNS_VIEW')")
    public ResponseEntity<ApiResponse<List<SalesReturnResponse>>> all() {
        List<SalesReturnResponse> returns = salesReturnService.getAllReturns();
        return ResponseEntity.ok(ApiResponse.success(returns, "All returns"));
    }

    @PostMapping("/pending")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_RETURNS_APPROVE')")
    public ResponseEntity<ApiResponse<List<SalesReturnResponse>>> pending() {
        List<SalesReturnResponse> returns = salesReturnService.getPendingReturns();
        return ResponseEntity.ok(ApiResponse.success(returns, "Pending returns"));
    }

    @PostMapping("/by-order/{orderId}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_RETURNS_VIEW')")
    public ResponseEntity<ApiResponse<List<SalesReturnResponse>>> byOrder(@PathVariable UUID orderId) {
        List<SalesReturnResponse> returns = salesReturnService.getReturnsByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(returns, "Returns for order"));
    }
}
