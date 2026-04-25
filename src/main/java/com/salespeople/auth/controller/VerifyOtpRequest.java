package com.salespeople.auth.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @NotNull(message = "User ID is required")
    private Long usrId;

    @NotBlank(message = "OTP code is required")
    @Size(min = 4, max = 4, message = "OTP must be 4 digits")
    private String usrSecret;
}
