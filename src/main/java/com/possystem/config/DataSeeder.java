package com.possystem.config;

import com.possystem.auth.user.Permission;
import com.possystem.auth.user.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        seedPermissions();
    }

    private void seedPermissions() {
        List<PermissionData> permissions = Arrays.asList(
                // Dashboard
                new PermissionData("DASHBOARD", "Dashboard", "Access to dashboard and analytics"),

                // User Management
                new PermissionData("USER_MANAGEMENT", "User Management", "Manage system users"),
                new PermissionData("PROFILE_MANAGEMENT", "Profile Management", "Manage user profiles and permissions"),

                // Tenant Management
                new PermissionData("TENANT_MANAGEMENT", "Tenant Management", "Manage tenants"),
                new PermissionData("SHOP_MANAGEMENT", "Shop Management", "Manage shops"),

                // POS Operations
                new PermissionData("POS_SALES", "POS Sales", "Process sales transactions"),
                new PermissionData("POS_RETURNS", "POS Returns", "Process returns and refunds"),
                new PermissionData("POS_VOID", "POS Void", "Void transactions"),

                // Inventory
                new PermissionData("INVENTORY", "Inventory", "Manage inventory"),
                new PermissionData("STOCK_TRANSFER", "Stock Transfer", "Transfer stock between locations"),
                new PermissionData("STOCK_ADJUSTMENT", "Stock Adjustment", "Adjust stock levels"),

                // Products
                new PermissionData("PRODUCT_MANAGEMENT", "Product Management", "Manage products and categories"),
                new PermissionData("PRICING", "Pricing", "Manage product pricing"),

                // Customers
                new PermissionData("CUSTOMER_MANAGEMENT", "Customer Management", "Manage customers"),
                new PermissionData("CUSTOMER_CREDIT", "Customer Credit", "Manage customer credit"),

                // Suppliers
                new PermissionData("SUPPLIER_MANAGEMENT", "Supplier Management", "Manage suppliers"),
                new PermissionData("PURCHASE_ORDERS", "Purchase Orders", "Manage purchase orders"),

                // Reports
                new PermissionData("REPORTS_SALES", "Sales Reports", "View sales reports"),
                new PermissionData("REPORTS_INVENTORY", "Inventory Reports", "View inventory reports"),
                new PermissionData("REPORTS_FINANCIAL", "Financial Reports", "View financial reports"),

                // Settings
                new PermissionData("SYSTEM_SETTINGS", "System Settings", "Manage system settings"),
                new PermissionData("PAYMENT_SETTINGS", "Payment Settings", "Manage payment methods"),
                new PermissionData("TAX_SETTINGS", "Tax Settings", "Manage tax configurations")
        );

        int created = 0;
        for (PermissionData data : permissions) {
            if (!permissionRepository.existsByPermissionCode(data.code)) {
                Permission permission = Permission.builder()
                        .permissionCode(data.code)
                        .permissionName(data.name)
                        .description(data.description)
                        .isActive(true)
                        .build();
                permissionRepository.save(permission);
                created++;
            }
        }

        if (created > 0) {
            log.info("Seeded {} permissions", created);
        }
    }

    private record PermissionData(String code, String name, String description) {}
}
