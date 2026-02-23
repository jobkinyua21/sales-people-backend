package com.possystem.businesstype;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class BusinessTypeResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private Boolean isActive;
    private List<ModuleInfo> defaultModules;
    private List<ModuleInfo> additionalModules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleInfo {
        private UUID moduleId;
        private String moduleCode;
        private String moduleName;
    }
}
