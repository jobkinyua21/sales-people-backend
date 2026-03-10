package com.possystem.inventory.stockalert;

import com.possystem.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-alerts")
@RequiredArgsConstructor
public class StockAlertController {

    private final StockAlertService stockAlertService;

    @PostMapping("/active")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('STOCK_ALERTS_VIEW')")
    public ResponseEntity<ApiResponse<List<StockAlertResponse>>> getActiveAlerts() {
        List<StockAlertResponse> alerts = stockAlertService.getActiveAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts, "Active stock alerts"));
    }

    @PostMapping("/all")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('STOCK_ALERTS_VIEW')")
    public ResponseEntity<ApiResponse<List<StockAlertResponse>>> getAllAlerts() {
        List<StockAlertResponse> alerts = stockAlertService.getAllAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts, "All stock alerts"));
    }

    @PostMapping("/count")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('STOCK_ALERTS_VIEW')")
    public ResponseEntity<ApiResponse<Long>> getActiveAlertCount() {
        long count = stockAlertService.getActiveAlertCount();
        return ResponseEntity.ok(ApiResponse.success(count, "Active alert count"));
    }

    @PostMapping("/acknowledge/{alertId}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('STOCK_ALERTS_MANAGE')")
    public ResponseEntity<ApiResponse<StockAlertResponse>> acknowledge(@PathVariable UUID alertId) {
        StockAlertResponse response = stockAlertService.acknowledgeAlert(alertId);
        return ResponseEntity.ok(ApiResponse.success(response, "Alert acknowledged"));
    }

    @PostMapping("/resolve/{alertId}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('STOCK_ALERTS_MANAGE')")
    public ResponseEntity<ApiResponse<StockAlertResponse>> resolve(@PathVariable UUID alertId) {
        StockAlertResponse response = stockAlertService.resolveAlert(alertId);
        return ResponseEntity.ok(ApiResponse.success(response, "Alert resolved"));
    }
}
