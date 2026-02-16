package com.possystem.menu;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuResponse {

    private UUID id;
    private String menuCode;
    private String menuName;
    private String menuLink;
    private String menuIcon;
    private UUID parentId;
    private Integer sortOrder;
    private String module;
    private List<MenuResponse> children;
}
