package com.possystem.kitchen.production;

import com.possystem.common.ApiResponse;
import com.possystem.security.RequiresModule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/production")
@RequiredArgsConstructor
@RequiresModule("KITCHEN")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PRODUCTION_CREATE')")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> create(@Valid @RequestBody ProductionOrderRequest request) {
        ProductionOrderResponse response = productionOrderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "Production order created"));
    }

    @PostMapping("/start/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PRODUCTION_MANAGE')")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> start(@PathVariable UUID id) {
        ProductionOrderResponse response = productionOrderService.start(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Production started — ingredients deducted"));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PRODUCTION_MANAGE')")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> complete(@Valid @RequestBody ProductionCompleteRequest request) {
        ProductionOrderResponse response = productionOrderService.complete(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Production completed — finished goods added to stock"));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PRODUCTION_MANAGE')")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> cancel(@Valid @RequestBody ProductionCancelRequest request) {
        ProductionOrderResponse response = productionOrderService.cancel(request.getProductionOrderId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success(response, "Production order cancelled"));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PRODUCTION_VIEW')")
    public ResponseEntity<ApiResponse<List<ProductionOrderResponse>>> fetch(@RequestBody ProductionFetchRequest request) {
        List<ProductionOrderResponse> orders = productionOrderService.fetch(request);
        return ResponseEntity.ok(ApiResponse.success(orders, "Production orders fetched"));
    }
}
