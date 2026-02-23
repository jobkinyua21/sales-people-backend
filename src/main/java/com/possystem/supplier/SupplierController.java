package com.possystem.supplier;

import com.possystem.common.ApiResponse;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<SupplierResponse>> save(
            @Valid @RequestBody SupplierRequest request) {
        SupplierResponse response = supplierService.save(request);
        String message = request.getId() != null ? "Supplier updated" : "Supplier created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    public ResponseEntity<ListResponse<SupplierResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<SupplierResponse> response = supplierService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        supplierService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Supplier deleted"));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        int count = supplierService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, count + " suppliers deleted"));
    }
}
