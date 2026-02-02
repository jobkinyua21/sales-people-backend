package com.possystem.generalsetting.users;

import com.possystem.common.UserStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateShopUserRequest {

    private String email;

    private String phoneNumber;

    private String firstName;

    private String lastName;

    private UUID profileId;

    private UserStatus status;

    private Boolean isActive;
}
