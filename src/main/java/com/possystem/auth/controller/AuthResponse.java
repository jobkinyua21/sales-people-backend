package com.possystem.auth.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Long expiresIn;

    private String userType;

    private UUID usrId;

    private String email;

    private String fullName;

    private UUID tenantId;

    private String tenantCode;

    private UUID sessionId;

    private UUID shopId;

    private String shopName;

    private Boolean mustChangePassword;
}
