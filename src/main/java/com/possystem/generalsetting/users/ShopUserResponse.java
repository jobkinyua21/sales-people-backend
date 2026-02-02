package com.possystem.generalsetting.users;

import com.possystem.common.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ShopUserResponse {

    private UUID userId;
    private UUID tenantId;
    private UUID shopId;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private UserStatus status;
    private UUID profileId;
    private String profileName;
    private LocalDateTime lastLogin;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static ShopUserResponse fromEntity(ShopUser user) {
        return ShopUserResponse.builder()
                .userId(user.getUserId())
                .tenantId(user.getTenantId())
                .shopId(user.getShopId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .profileId(user.getPosProfile() != null ? user.getPosProfile().getProfileId() : null)
                .profileName(user.getPosProfile() != null ? user.getPosProfile().getProfileName() : null)
                .lastLogin(user.getLastLogin())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();
    }
}
