package com.salespeople.role;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.salespeople.permission.PermissionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleResponse {

    private UUID id;
    private String roleCode;
    private String roleName;
    private RoleType roleType;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<PermissionResponse> permissions;
    private long userCount;
}
