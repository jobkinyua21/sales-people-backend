package com.possystem.menu;

import com.possystem.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping("/fetch")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> fetch() {
        List<MenuResponse> menus = menuService.fetchMenuTree();
        return ResponseEntity.ok(ApiResponse.success(menus, "Menu fetched"));
    }
}
