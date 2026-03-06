package com.possystem.inventory;

import com.possystem.common.ApiResponse;
import com.possystem.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/stock")
@RequiredArgsConstructor
public class InventoryStockController {

    private final InventoryStockService inventoryStockService;
    private final InventoryStockExcelService inventoryStockExcelService;
    private final InventoryStockExportService inventoryStockExportService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('INVENTORY_CREATE') or hasAuthority('INVENTORY_EDIT')")
    public ResponseEntity<ApiResponse<InventoryStockResponse>> save(
            @Valid @RequestBody InventoryStockRequest request) {
        InventoryStockResponse response = inventoryStockService.save(request);
        String message = request.getId() != null ? "Stock updated" : "Stock created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('INVENTORY_VIEW')")
    public ResponseEntity<ListResponse<InventoryStockResponse>> fetch(
            @RequestBody InventoryStockFetchRequest request) {
        ListResponse<InventoryStockResponse> response = inventoryStockService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('INVENTORY_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        inventoryStockService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock record deleted"));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('INVENTORY_DELETE')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        int count = inventoryStockService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, count + " stock records deleted"));
    }

    @PostMapping("/template")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('INVENTORY_CREATE')")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] templateBytes = inventoryStockExcelService.generateTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "stock_update_template.xlsx");
        headers.setContentLength(templateBytes.length);

        return new ResponseEntity<>(templateBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('INVENTORY_CREATE')")
    public ResponseEntity<ApiResponse<StockUploadResponse>> uploadStock(
            @RequestParam("file") MultipartFile file) {

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        StockUploadResponse response = inventoryStockExcelService.processUpload(file);

        if (response.getFailureCount() > 0) {
            String message = String.format("Upload failed: %d errors found. No stock was updated.",
                    response.getFailureCount());
            return ResponseEntity.badRequest().body(ApiResponse.success(response, message));
        }

        String message = String.format("%d stock records updated successfully", response.getSuccessCount());
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/export/excel")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('INVENTORY_EXPORT')")
    public ResponseEntity<byte[]> exportExcel(@RequestBody InventoryStockFetchRequest request) {
        byte[] bytes = inventoryStockExportService.exportExcel(
                request.getSearch(), request.getCategoryId(), request.getStockStatus());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "inventory_report.xlsx");
        headers.setContentLength(bytes.length);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('INVENTORY_EXPORT')")
    public ResponseEntity<byte[]> exportPdf(@RequestBody InventoryStockFetchRequest request) {
        byte[] bytes = inventoryStockExportService.exportPdf(
                request.getSearch(), request.getCategoryId(), request.getStockStatus());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "inventory_report.pdf");
        headers.setContentLength(bytes.length);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
