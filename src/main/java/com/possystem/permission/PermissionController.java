package com.possystem.permission;

import com.possystem.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping("/fetch")
    public ResponseEntity<ApiResponse<Map<String, List<PermissionResponse>>>> fetch() {
        Map<String, List<PermissionResponse>> permissions = permissionService.fetchAll();
        return ResponseEntity.ok(ApiResponse.success(permissions, "Permissions fetched"));
    }
}
