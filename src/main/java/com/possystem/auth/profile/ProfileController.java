package com.possystem.auth.profile;

import com.possystem.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/profiles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Profile Management", description = "Manage user profiles and permissions")
@SecurityRequirement(name = "Bearer Authentication")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Create profile", description = "Create a new user profile")
    @PostMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(
            @Valid @RequestBody CreateProfileRequest request) {
        ProfileResponse response = profileService.createProfile(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Profile created successfully"));
    }

    @Operation(summary = "Update profile", description = "Update an existing profile")
    @PutMapping("/{profileId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = profileService.updateProfile(profileId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }

    @Operation(summary = "Get profile", description = "Get a profile by ID")
    @GetMapping("/{profileId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@PathVariable UUID profileId) {
        ProfileResponse response = profileService.getProfile(profileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get all profiles", description = "Get all profiles")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProfileResponse>>> getAllProfiles() {
        List<ProfileResponse> response = profileService.getAllProfiles();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Assign permissions to profile", description = "Assign or update permissions for a profile")
    @PostMapping("/{profileId}/permissions")
    public ResponseEntity<ApiResponse<ProfileResponse>> assignPermissions(
            @PathVariable UUID profileId,
            @Valid @RequestBody List<ProfilePermissionRequest> permissions) {
        ProfileResponse response = profileService.assignPermissions(profileId, permissions);
        return ResponseEntity.ok(ApiResponse.success(response, "Permissions assigned successfully"));
    }

    @Operation(summary = "Remove permission from profile", description = "Remove a permission from a profile")
    @DeleteMapping("/{profileId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<Void>> removePermission(
            @PathVariable UUID profileId,
            @PathVariable UUID permissionId) {
        profileService.removePermission(profileId, permissionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Permission removed successfully"));
    }

    @Operation(summary = "Assign profile to user", description = "Assign a profile to a user")
    @PostMapping("/assign-user")
    public ResponseEntity<ApiResponse<Void>> assignProfileToUser(
            @Valid @RequestBody AssignUserProfileRequest request) {
        profileService.assignProfileToUser(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Profile assigned to user successfully"));
    }

    @Operation(summary = "Get all permissions", description = "Get all available permissions")
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        List<PermissionResponse> response = profileService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
