package com.possystem.purchasing.returns;

import com.possystem.common.ApiResponse;
import com.possystem.common.ListResponse;
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
@RequestMapping("/api/v1/purchase-returns")
@RequiredArgsConstructor
@RequiresModule("PURCHASING")
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_RETURNS_CREATE') or hasAuthority('PURCHASE_RETURNS_EDIT')")
    public ResponseEntity<ApiResponse<PurchaseReturnResponse>> save(
            @Valid @RequestBody PurchaseReturnRequest request) {
        PurchaseReturnResponse response = purchaseReturnService.save(request);
        String message = request.getId() != null ? "Purchase return updated" : "Purchase return created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_RETURNS_VIEW')")
    public ResponseEntity<ListResponse<PurchaseReturnResponse>> fetch(
            @RequestBody PurchaseReturnFetchRequest request) {
        ListResponse<PurchaseReturnResponse> response = purchaseReturnService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/returnable-items")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_RETURNS_VIEW')")
    public ResponseEntity<ApiResponse<List<ReturnableItemResponse>>> returnableItems(
            @Valid @RequestBody PurchaseReturnActionRequest request) {
        List<ReturnableItemResponse> items = purchaseReturnService.getReturnableItems(request.getId());
        return ResponseEntity.ok(ApiResponse.success(items, "Returnable items fetched"));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_RETURNS_EDIT')")
    public ResponseEntity<ApiResponse<PurchaseReturnResponse>> submit(
            @Valid @RequestBody PurchaseReturnActionRequest request) {
        PurchaseReturnResponse response = purchaseReturnService.submitReturn(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase return submitted"));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_RETURNS_EDIT')")
    public ResponseEntity<ApiResponse<PurchaseReturnResponse>> complete(
            @Valid @RequestBody PurchaseReturnActionRequest request) {
        PurchaseReturnResponse response = purchaseReturnService.completeReturn(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase return completed. Stock deducted."));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_RETURNS_EDIT')")
    public ResponseEntity<ApiResponse<PurchaseReturnResponse>> cancel(
            @Valid @RequestBody PurchaseReturnActionRequest request) {
        PurchaseReturnResponse response = purchaseReturnService.cancelReturn(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase return cancelled"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_RETURNS_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        purchaseReturnService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Purchase return deleted"));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_RETURNS_DELETE')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        int count = purchaseReturnService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, count + " purchase return(s) deleted"));
    }
}
