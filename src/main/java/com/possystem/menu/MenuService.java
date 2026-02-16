package com.possystem.menu;

import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    public List<MenuResponse> fetchMenuTree() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        List<Menu> allMenus = menuRepository.findByIsActiveTrueOrderBySortOrder();

        // Extract user's permission modules (e.g., CUSTOMERS_VIEW -> CUSTOMERS)
        Set<String> userModules = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .map(a -> a.contains("_") ? a.substring(0, a.lastIndexOf('_')) : a)
                .collect(Collectors.toSet());

        // Filter menus: include if module is null (always visible like Dashboard) or module matches user's permissions
        List<Menu> filteredMenus = allMenus.stream()
                .filter(menu -> menu.getModule() == null || userModules.contains(menu.getModule()))
                .toList();

        return buildTree(filteredMenus);
    }

    private List<MenuResponse> buildTree(List<Menu> flatMenus) {
        Map<UUID, MenuResponse> menuMap = new LinkedHashMap<>();
        List<MenuResponse> rootMenus = new ArrayList<>();

        for (Menu menu : flatMenus) {
            MenuResponse response = MenuResponse.builder()
                    .id(menu.getId())
                    .menuCode(menu.getMenuCode())
                    .menuName(menu.getMenuName())
                    .menuLink(menu.getMenuLink())
                    .menuIcon(menu.getMenuIcon())
                    .parentId(menu.getParentId())
                    .sortOrder(menu.getSortOrder())
                    .module(menu.getModule())
                    .children(new ArrayList<>())
                    .build();
            menuMap.put(menu.getId(), response);
        }

        for (MenuResponse response : menuMap.values()) {
            if (response.getParentId() == null) {
                rootMenus.add(response);
            } else {
                MenuResponse parent = menuMap.get(response.getParentId());
                if (parent != null) {
                    parent.getChildren().add(response);
                }
            }
        }

        // Remove empty children lists for cleaner JSON
        cleanEmptyChildren(rootMenus);

        return rootMenus;
    }

    private void cleanEmptyChildren(List<MenuResponse> menus) {
        for (MenuResponse menu : menus) {
            if (menu.getChildren() != null && menu.getChildren().isEmpty()) {
                menu.setChildren(null);
            } else if (menu.getChildren() != null) {
                cleanEmptyChildren(menu.getChildren());
            }
        }
    }
}
