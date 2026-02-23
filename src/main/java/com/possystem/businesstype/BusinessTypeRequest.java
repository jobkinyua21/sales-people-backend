package com.possystem.businesstype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTypeRequest {

    private UUID id;

    @NotBlank(message = "Business type name is required")
    private String name;

    private String description;

    private String iconUrl;

    private List<ModuleEntry> modules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleEntry {
        @NotNull(message = "Module ID is required")
        private UUID moduleId;

        @Builder.Default
        private Boolean isDefault = false;
    }
}
