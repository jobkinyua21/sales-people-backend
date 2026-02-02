package com.possystem.auth.profile;

import com.possystem.auth.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final PermissionRepository permissionRepository;
    private final ProfilePermissionRepository profilePermissionRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProfileResponse createProfile(CreateProfileRequest request) {
        if (profileRepository.existsByProfileCode(request.getProfileCode())) {
            throw new IllegalArgumentException("Profile code already exists");
        }
        if (profileRepository.existsByProfileName(request.getProfileName())) {
            throw new IllegalArgumentException("Profile name already exists");
        }

        Profile profile = Profile.builder()
                .profileName(request.getProfileName())
                .profileCode(request.getProfileCode().toUpperCase())
                .description(request.getDescription())
                .isActive(true)
                .build();

        Profile saved = profileRepository.save(profile);
        return mapToResponse(saved);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID profileId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        if (request.getProfileName() != null) {
            profile.setProfileName(request.getProfileName());
        }
        if (request.getDescription() != null) {
            profile.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            profile.setIsActive(request.getIsActive());
        }

        Profile saved = profileRepository.save(profile);
        return mapToResponse(saved);
    }

    public ProfileResponse getProfile(UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        return mapToResponse(profile);
    }

    public List<ProfileResponse> getAllProfiles() {
        return profileRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfileResponse assignPermissions(UUID profileId, List<ProfilePermissionRequest> permissions) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        for (ProfilePermissionRequest permRequest : permissions) {
            Permission permission = permissionRepository.findById(permRequest.getPermissionId())
                    .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permRequest.getPermissionId()));

            ProfilePermission profilePermission = profilePermissionRepository
                    .findByProfileAndPermission(profile, permission)
                    .orElse(ProfilePermission.builder()
                            .profile(profile)
                            .permission(permission)
                            .build());

            profilePermission.setCanRead(permRequest.getCanRead());
            profilePermission.setCanWrite(permRequest.getCanWrite());
            profilePermission.setCanCreate(permRequest.getCanCreate());
            profilePermission.setCanApprove(permRequest.getCanApprove());
            profilePermission.setCanExport(permRequest.getCanExport());

            profilePermissionRepository.save(profilePermission);
        }

        return mapToResponse(profile);
    }

    @Transactional
    public void removePermission(UUID profileId, UUID permissionId) {
        ProfilePermission profilePermission = profilePermissionRepository
                .findByProfileProfileIdAndPermissionPermissionId(profileId, permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not assigned to this profile"));

        profilePermissionRepository.delete(profilePermission);
    }

    @Transactional
    public void assignProfileToUser(AssignUserProfileRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Profile profile = profileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        user.setProfile(profile);
        userRepository.save(user);
    }

    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
    }

    private ProfileResponse mapToResponse(Profile profile) {
        List<ProfilePermission> profilePermissions = profilePermissionRepository.findByProfile(profile);

        List<ProfileResponse.PermissionDetail> permissionDetails = profilePermissions.stream()
                .map(pp -> ProfileResponse.PermissionDetail.builder()
                        .permissionId(pp.getPermission().getPermissionId())
                        .permissionName(pp.getPermission().getPermissionName())
                        .permissionCode(pp.getPermission().getPermissionCode())
                        .canRead(pp.getCanRead())
                        .canWrite(pp.getCanWrite())
                        .canCreate(pp.getCanCreate())
                        .canApprove(pp.getCanApprove())
                        .canExport(pp.getCanExport())
                        .build())
                .collect(Collectors.toList());

        return ProfileResponse.builder()
                .profileId(profile.getProfileId())
                .profileName(profile.getProfileName())
                .profileCode(profile.getProfileCode())
                .description(profile.getDescription())
                .isActive(profile.getIsActive())
                .permissions(permissionDetails)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private PermissionResponse mapToPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .permissionId(permission.getPermissionId())
                .permissionName(permission.getPermissionName())
                .permissionCode(permission.getPermissionCode())
                .description(permission.getDescription())
                .isActive(permission.getIsActive())
                .build();
    }
}
