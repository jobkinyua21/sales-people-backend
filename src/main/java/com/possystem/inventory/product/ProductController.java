package com.possystem.inventory;

import com.possystem.common.ApiResponse;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductExcelService productExcelService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<ProductResponse>> save(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.save(request);
        String message = request.getId() != null ? "Product updated" : "Product created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    public ResponseEntity<ListResponse<ProductResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<ProductResponse> response = productService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted"));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        int count = productService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, count + " products deleted"));
    }

    @PostMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] templateBytes = productExcelService.generateTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "product_upload_template.xlsx");
        headers.setContentLength(templateBytes.length);

        return new ResponseEntity<>(templateBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ProductUploadResponse>> uploadProducts(
            @RequestParam("file") MultipartFile file) {

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        ProductUploadResponse response = productExcelService.processUpload(file);

        if (response.getFailureCount() > 0) {
            String message = String.format("Upload failed: %d errors found. No products were saved.",
                    response.getFailureCount());
            return ResponseEntity.badRequest().body(ApiResponse.success(response, message));
        }

        String message = String.format("%d products uploaded successfully", response.getSuccessCount());
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
