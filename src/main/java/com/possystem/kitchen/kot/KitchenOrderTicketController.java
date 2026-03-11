package com.possystem.kitchen.kot;

import com.possystem.common.ApiResponse;
import com.possystem.kitchen.recipe.PrepStation;
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
@RequestMapping("/api/v1/kitchen-orders")
@RequiredArgsConstructor
@RequiresModule("KITCHEN")
public class KitchenOrderTicketController {

    private final KitchenOrderTicketService kotService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('KITCHEN_ORDERS_CREATE')")
    public ResponseEntity<ApiResponse<KotResponse>> create(@Valid @RequestBody KotRequest request) {
        KotResponse response = kotService.createKot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "Kitchen order ticket created"));
    }

    @PostMapping("/accept/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('KITCHEN_ORDERS_MANAGE')")
    public ResponseEntity<ApiResponse<KotResponse>> accept(@PathVariable UUID id) {
        KotResponse response = kotService.acceptKot(id);
        return ResponseEntity.ok(ApiResponse.success(response, "KOT accepted — ingredients deducted"));
    }

    @PostMapping("/update-item-status")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('KITCHEN_ORDERS_MANAGE')")
    public ResponseEntity<ApiResponse<KotResponse>> updateItemStatus(@Valid @RequestBody KotUpdateItemStatusRequest request) {
        KotResponse response = kotService.updateItemStatus(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Item status updated"));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('KITCHEN_ORDERS_MANAGE')")
    public ResponseEntity<ApiResponse<KotResponse>> cancel(@Valid @RequestBody KotCancelRequest request) {
        KotResponse response = kotService.cancelKot(request);
        return ResponseEntity.ok(ApiResponse.success(response, "KOT cancelled"));
    }

    @PostMapping("/active")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('KITCHEN_ORDERS_VIEW')")
    public ResponseEntity<ApiResponse<List<KotResponse>>> active() {
        List<KotResponse> kots = kotService.getActiveKots();
        return ResponseEntity.ok(ApiResponse.success(kots, "Active kitchen orders"));
    }

    @PostMapping("/pending")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('KITCHEN_ORDERS_VIEW')")
    public ResponseEntity<ApiResponse<List<KotResponse>>> pending() {
        List<KotResponse> kots = kotService.getPendingKots();
        return ResponseEntity.ok(ApiResponse.success(kots, "Pending kitchen orders"));
    }

    @PostMapping("/by-order/{orderId}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('KITCHEN_ORDERS_VIEW')")
    public ResponseEntity<ApiResponse<List<KotResponse>>> byOrder(@PathVariable UUID orderId) {
        List<KotResponse> kots = kotService.getKotsByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(kots, "KOTs for order"));
    }

    @PostMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('KITCHEN_ORDERS_VIEW')")
    public ResponseEntity<ApiResponse<KotResponse>> detail(@PathVariable UUID id) {
        KotResponse response = kotService.getKotById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "KOT details"));
    }

    @PostMapping("/by-station/{station}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('KITCHEN_ORDERS_VIEW')")
    public ResponseEntity<ApiResponse<List<KotResponse.KotItemResponse>>> byStation(@PathVariable PrepStation station) {
        List<KotResponse.KotItemResponse> items = kotService.getItemsByStation(station);
        return ResponseEntity.ok(ApiResponse.success(items, "Items for station"));
    }
}
