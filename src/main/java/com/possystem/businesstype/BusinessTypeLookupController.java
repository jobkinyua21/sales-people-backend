package com.possystem.businesstype;

import com.possystem.common.ApiResponse;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-types")
@RequiredArgsConstructor
public class BusinessTypeLookupController {

    private final BusinessTypeService businessTypeService;

    @PostMapping("/fetch")
    public ResponseEntity<ListResponse<BusinessTypeResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<BusinessTypeResponse> response = businessTypeService.fetch(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/additional-modules/{id}")
    public ResponseEntity<ApiResponse<List<BusinessTypeResponse.ModuleInfo>>> getAdditionalModules(
            @PathVariable UUID id) {
        List<BusinessTypeResponse.ModuleInfo> modules = businessTypeService.getAvailableModules(id);
        return ResponseEntity.ok(ApiResponse.success(modules, "Additional modules fetched"));
    }
}
