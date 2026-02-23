package com.possystem.businesstype;

import com.possystem.common.ApiResponse;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/business-types")
@RequiredArgsConstructor
public class BusinessTypeController {

    private final BusinessTypeService businessTypeService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<BusinessTypeResponse>> save(
            @Valid @RequestBody BusinessTypeRequest request) {
        BusinessTypeResponse response = businessTypeService.save(request);
        String message = request.getId() != null ? "Business type updated" : "Business type created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    public ResponseEntity<ListResponse<BusinessTypeResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<BusinessTypeResponse> response = businessTypeService.fetch(request);
        return ResponseEntity.ok(response);
    }

@DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        businessTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Business type deleted"));
    }
}
