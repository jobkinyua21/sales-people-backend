package com.possystem.module;

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
@RequestMapping("/api/v1/additional-modules")
@RequiredArgsConstructor
public class AdditionalModuleController {

    private final AdditionalModuleService additionalModuleService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<AdditionalModuleResponse>> save(
            @Valid @RequestBody AdditionalModuleRequest request) {
        AdditionalModuleResponse response = additionalModuleService.save(request);
        String message = request.getId() != null ? "Module updated" : "Module created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    public ResponseEntity<ListResponse<AdditionalModuleResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<AdditionalModuleResponse> response = additionalModuleService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        additionalModuleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Module deleted"));
    }
}
