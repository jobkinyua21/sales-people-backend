package com.possystem.shop;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.common.UserStatus;
import com.possystem.common.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopUserResponse {

    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UserType userType;
    private UserStatus status;
    private UUID roleId;
    private UUID shopId;
    private String shopName;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
