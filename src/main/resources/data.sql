-- Seed message templates (only insert if not already present)

INSERT INTO pos_core.message_template (mst_name, mst_description, mst_email, mst_sms, mst_subject, mst_type, mst_status, mst_content, created_at, updated_at)
SELECT 'Login OTP', 'OTP sent during login for verification',
'<html><body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
<div style="max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
<div style="background-color: #2563eb; padding: 30px; text-align: center;">
<h1 style="color: #ffffff; margin: 0; font-size: 24px;">POS Management System</h1>
</div>
<div style="padding: 30px;">
<h2 style="color: #333333; margin-top: 0;">Hello #{USER_FIRST_NAME},</h2>
<p style="color: #555555; font-size: 16px; line-height: 1.5;">Your one-time verification code is:</p>
<div style="text-align: center; margin: 30px 0;">
<span style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #2563eb; background-color: #eff6ff; padding: 15px 30px; border-radius: 8px; border: 2px dashed #2563eb;">#{OTP_CODE}</span>
</div>
<p style="color: #555555; font-size: 14px; line-height: 1.5;">This code expires in <strong>#{EXPIRY_MINUTES} minutes</strong>. Do not share this code with anyone.</p>
<p style="color: #999999; font-size: 12px; margin-top: 30px;">If you did not request this code, please ignore this email.</p>
</div>
<div style="background-color: #f8f9fa; padding: 15px; text-align: center;">
<p style="color: #999999; font-size: 11px; margin: 0;">POS Management System. All rights reserved.</p>
</div>
</div>
</body></html>',
'Your OTP code is #{OTP_CODE}. It expires in #{EXPIRY_MINUTES} minutes.',
'Your Verification Code',
'LOGIN_OTP', 'ACTIVE', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pos_core.message_template WHERE mst_type = 'LOGIN_OTP');

INSERT INTO pos_core.message_template (mst_name, mst_description, mst_email, mst_sms, mst_subject, mst_type, mst_status, mst_content, created_at, updated_at)
SELECT 'Password Reset', 'OTP sent during forgot password flow',
'<html><body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
<div style="max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
<div style="background-color: #2563eb; padding: 30px; text-align: center;">
<h1 style="color: #ffffff; margin: 0; font-size: 24px;">POS Management System</h1>
</div>
<div style="padding: 30px;">
<h2 style="color: #333333; margin-top: 0;">Hello #{USER_FIRST_NAME},</h2>
<p style="color: #555555; font-size: 16px; line-height: 1.5;">We received a request to reset your password. Your verification code is:</p>
<div style="text-align: center; margin: 30px 0;">
<span style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #2563eb; background-color: #eff6ff; padding: 15px 30px; border-radius: 8px; border: 2px dashed #2563eb;">#{OTP_CODE}</span>
</div>
<p style="color: #555555; font-size: 14px; line-height: 1.5;">This code expires in <strong>#{EXPIRY_MINUTES} minutes</strong>.</p>
<p style="color: #999999; font-size: 12px; margin-top: 30px;">If you did not request a password reset, please ignore this email.</p>
</div>
<div style="background-color: #f8f9fa; padding: 15px; text-align: center;">
<p style="color: #999999; font-size: 11px; margin: 0;">POS Management System. All rights reserved.</p>
</div>
</div>
</body></html>',
'Your password reset code is #{OTP_CODE}. It expires in #{EXPIRY_MINUTES} minutes.',
'Password Reset - Verification Code',
'PASSWORD_RESET', 'ACTIVE', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pos_core.message_template WHERE mst_type = 'PASSWORD_RESET');

-- ==================== MENU SEED DATA ====================

-- Root menus
INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000001', 'MN-DASHBOARD', 'Dashboard', '/dashboard', 'dashboard', NULL, 1, NULL, true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000002', 'MN-CUSTOMERS', 'Customers', '/customers', 'users', NULL, 2, 'CUSTOMERS', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000003', 'MN-PRODUCTS', 'Products', '/products', 'package', NULL, 3, 'PRODUCTS', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000004', 'MN-INVOICES', 'Invoices', '/invoices', 'file-text', NULL, 4, 'INVOICES', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000005', 'MN-PAYMENTS', 'Payments', '/payments', 'credit-card', NULL, 5, 'PAYMENTS', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000006', 'MN-REPORTS', 'Reports', '/reports', 'bar-chart', NULL, 6, 'REPORTS', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000007', 'MN-SETTINGS', 'Settings', '/settings', 'settings', NULL, 7, 'SETTINGS', true)
ON CONFLICT (menu_code) DO NOTHING;

-- Products children
INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000031', 'MN-PRODUCT-LIST', 'Product List', '/products/list', 'list', 'a0000000-0000-0000-0000-000000000003', 1, 'PRODUCTS', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000032', 'MN-CATEGORIES', 'Categories', '/products/categories', 'tag', 'a0000000-0000-0000-0000-000000000003', 2, 'PRODUCTS', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000033', 'MN-INVENTORY', 'Inventory', '/products/inventory', 'archive', 'a0000000-0000-0000-0000-000000000003', 3, 'PRODUCTS', true)
ON CONFLICT (menu_code) DO NOTHING;

-- Reports children
INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000061', 'MN-REVENUE', 'Revenue', '/reports/revenue', 'trending-up', 'a0000000-0000-0000-0000-000000000006', 1, 'REPORTS', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000062', 'MN-SALES', 'Sales', '/reports/sales', 'shopping-cart', 'a0000000-0000-0000-0000-000000000006', 2, 'REPORTS', true)
ON CONFLICT (menu_code) DO NOTHING;

-- Settings children
INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000071', 'MN-USERS', 'Users', '/settings/users', 'user-plus', 'a0000000-0000-0000-0000-000000000007', 1, 'USERS', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000072', 'MN-ROLES', 'Roles', '/settings/roles', 'shield', 'a0000000-0000-0000-0000-000000000007', 2, 'ROLES', true)
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO pos_core.menu (id, menu_code, menu_name, menu_link, menu_icon, parent_id, sort_order, module, is_active)
VALUES ('a0000000-0000-0000-0000-000000000073', 'MN-SHOP-PROFILE', 'Shop Profile', '/settings/shop-profile', 'store', 'a0000000-0000-0000-0000-000000000007', 3, 'SHOP_PROFILE', true)
ON CONFLICT (menu_code) DO NOTHING;

-- ==================== PERMISSION SEED DATA ====================

-- CUSTOMERS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CUSTOMERS_VIEW', 'View Customers', 'CUSTOMERS', 'VIEW', 'View customer list and details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CUSTOMERS_CREATE', 'Create Customers', 'CUSTOMERS', 'CREATE', 'Add new customers')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CUSTOMERS_EDIT', 'Edit Customers', 'CUSTOMERS', 'EDIT', 'Modify customer information')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CUSTOMERS_DELETE', 'Delete Customers', 'CUSTOMERS', 'DELETE', 'Remove customers')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CUSTOMERS_EXPORT', 'Export Customers', 'CUSTOMERS', 'EXPORT', 'Export customer data')
ON CONFLICT (permission_code) DO NOTHING;

-- PRODUCTS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PRODUCTS_VIEW', 'View Products', 'PRODUCTS', 'VIEW', 'View product list, categories and inventory')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PRODUCTS_CREATE', 'Create Products', 'PRODUCTS', 'CREATE', 'Add new products and categories')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PRODUCTS_EDIT', 'Edit Products', 'PRODUCTS', 'EDIT', 'Modify product information and inventory')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PRODUCTS_DELETE', 'Delete Products', 'PRODUCTS', 'DELETE', 'Remove products and categories')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PRODUCTS_EXPORT', 'Export Products', 'PRODUCTS', 'EXPORT', 'Export product data')
ON CONFLICT (permission_code) DO NOTHING;

-- INVOICES permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVOICES_VIEW', 'View Invoices', 'INVOICES', 'VIEW', 'View invoice list and details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVOICES_CREATE', 'Create Invoices', 'INVOICES', 'CREATE', 'Generate new invoices')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVOICES_EDIT', 'Edit Invoices', 'INVOICES', 'EDIT', 'Modify invoice details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVOICES_DELETE', 'Delete Invoices', 'INVOICES', 'DELETE', 'Remove invoices')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVOICES_EXPORT', 'Export Invoices', 'INVOICES', 'EXPORT', 'Export invoice data')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVOICES_MANAGE', 'Manage Invoices', 'INVOICES', 'MANAGE', 'Approve, void and manage invoice lifecycle')
ON CONFLICT (permission_code) DO NOTHING;

-- PAYMENTS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PAYMENTS_VIEW', 'View Payments', 'PAYMENTS', 'VIEW', 'View payment records')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PAYMENTS_CREATE', 'Create Payments', 'PAYMENTS', 'CREATE', 'Record new payments')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PAYMENTS_EDIT', 'Edit Payments', 'PAYMENTS', 'EDIT', 'Modify payment records')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PAYMENTS_DELETE', 'Delete Payments', 'PAYMENTS', 'DELETE', 'Remove payment records')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PAYMENTS_EXPORT', 'Export Payments', 'PAYMENTS', 'EXPORT', 'Export payment data')
ON CONFLICT (permission_code) DO NOTHING;

-- REPORTS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'REPORTS_VIEW', 'View Reports', 'REPORTS', 'VIEW', 'View revenue and sales reports')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'REPORTS_EXPORT', 'Export Reports', 'REPORTS', 'EXPORT', 'Export report data')
ON CONFLICT (permission_code) DO NOTHING;

-- USERS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'USERS_VIEW', 'View Users', 'USERS', 'VIEW', 'View user list and details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'USERS_CREATE', 'Create Users', 'USERS', 'CREATE', 'Add new users')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'USERS_EDIT', 'Edit Users', 'USERS', 'EDIT', 'Modify user information')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'USERS_DELETE', 'Delete Users', 'USERS', 'DELETE', 'Remove users')
ON CONFLICT (permission_code) DO NOTHING;

-- ROLES permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'ROLES_VIEW', 'View Roles', 'ROLES', 'VIEW', 'View roles and permissions')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'ROLES_CREATE', 'Create Roles', 'ROLES', 'CREATE', 'Create new roles')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'ROLES_EDIT', 'Edit Roles', 'ROLES', 'EDIT', 'Modify role permissions')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'ROLES_DELETE', 'Delete Roles', 'ROLES', 'DELETE', 'Remove custom roles')
ON CONFLICT (permission_code) DO NOTHING;

-- SETTINGS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SETTINGS_VIEW', 'View Settings', 'SETTINGS', 'VIEW', 'View system settings')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SETTINGS_EDIT', 'Edit Settings', 'SETTINGS', 'EDIT', 'Modify system settings')
ON CONFLICT (permission_code) DO NOTHING;

-- SHOP_PROFILE permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SHOP_PROFILE_VIEW', 'View Shop Profile', 'SHOP_PROFILE', 'VIEW', 'View shop profile details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SHOP_PROFILE_EDIT', 'Edit Shop Profile', 'SHOP_PROFILE', 'EDIT', 'Modify shop profile details')
ON CONFLICT (permission_code) DO NOTHING;

-- SUPPLIERS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIERS_VIEW', 'View Suppliers', 'SUPPLIERS', 'VIEW', 'View supplier list and details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIERS_CREATE', 'Create Suppliers', 'SUPPLIERS', 'CREATE', 'Add new suppliers')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIERS_EDIT', 'Edit Suppliers', 'SUPPLIERS', 'EDIT', 'Modify supplier information')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIERS_DELETE', 'Delete Suppliers', 'SUPPLIERS', 'DELETE', 'Remove suppliers')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIERS_EXPORT', 'Export Suppliers', 'SUPPLIERS', 'EXPORT', 'Export supplier data')
ON CONFLICT (permission_code) DO NOTHING;

-- PURCHASE_ORDERS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_ORDERS_VIEW', 'View Purchase Orders', 'PURCHASE_ORDERS', 'VIEW', 'View purchase order list and details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_ORDERS_CREATE', 'Create Purchase Orders', 'PURCHASE_ORDERS', 'CREATE', 'Create new purchase orders')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_ORDERS_EDIT', 'Edit Purchase Orders', 'PURCHASE_ORDERS', 'EDIT', 'Modify purchase order details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_ORDERS_DELETE', 'Delete Purchase Orders', 'PURCHASE_ORDERS', 'DELETE', 'Remove purchase orders')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_ORDERS_EXPORT', 'Export Purchase Orders', 'PURCHASE_ORDERS', 'EXPORT', 'Export purchase order data')
ON CONFLICT (permission_code) DO NOTHING;

-- Purchase payment permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_PAYMENTS_VIEW', 'View Purchase Payments', 'PURCHASE_PAYMENTS', 'VIEW', 'View purchase payment records')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_PAYMENTS_CREATE', 'Create Purchase Payments', 'PURCHASE_PAYMENTS', 'CREATE', 'Record payments to suppliers')
ON CONFLICT (permission_code) DO NOTHING;

-- Supplier invoice permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIER_INVOICES_VIEW', 'View Supplier Invoices', 'SUPPLIER_INVOICES', 'VIEW', 'View supplier invoices')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIER_INVOICES_CREATE', 'Create Supplier Invoices', 'SUPPLIER_INVOICES', 'CREATE', 'Create new supplier invoices')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIER_INVOICES_EDIT', 'Edit Supplier Invoices', 'SUPPLIER_INVOICES', 'EDIT', 'Modify supplier invoices')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIER_INVOICES_DELETE', 'Delete Supplier Invoices', 'SUPPLIER_INVOICES', 'DELETE', 'Delete supplier invoices')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SUPPLIER_INVOICES_APPROVE', 'Approve Supplier Invoices', 'SUPPLIER_INVOICES', 'MANAGE', 'Approve supplier invoices for payment')
ON CONFLICT (permission_code) DO NOTHING;

-- GRN permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'GRN_VIEW', 'View GRN', 'GRN', 'VIEW', 'View goods received notes')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'GRN_CREATE', 'Create GRN', 'GRN', 'CREATE', 'Create new goods received notes')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'GRN_EDIT', 'Edit GRN', 'GRN', 'EDIT', 'Modify goods received notes')
ON CONFLICT (permission_code) DO NOTHING;

-- PURCHASE_RETURNS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_RETURNS_VIEW', 'View Purchase Returns', 'PURCHASE_RETURNS', 'VIEW', 'View purchase return list and details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_RETURNS_CREATE', 'Create Purchase Returns', 'PURCHASE_RETURNS', 'CREATE', 'Create new purchase returns')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_RETURNS_EDIT', 'Edit Purchase Returns', 'PURCHASE_RETURNS', 'EDIT', 'Modify purchase return details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'PURCHASE_RETURNS_DELETE', 'Delete Purchase Returns', 'PURCHASE_RETURNS', 'DELETE', 'Remove purchase returns')
ON CONFLICT (permission_code) DO NOTHING;

-- EXPENSES permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSES_VIEW', 'View Expenses', 'EXPENSES', 'VIEW', 'View expense list and details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSES_CREATE', 'Create Expenses', 'EXPENSES', 'CREATE', 'Create new expenses')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSES_EDIT', 'Edit Expenses', 'EXPENSES', 'EDIT', 'Modify expense details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSES_DELETE', 'Delete Expenses', 'EXPENSES', 'DELETE', 'Remove expenses')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSES_EXPORT', 'Export Expenses', 'EXPENSES', 'EXPORT', 'Export expense data')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSES_MANAGE', 'Manage Expenses', 'EXPENSES', 'MANAGE', 'Approve, reject and manage expense lifecycle')
ON CONFLICT (permission_code) DO NOTHING;

-- EXPENSE_CATEGORIES permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSE_CATEGORIES_VIEW', 'View Expense Categories', 'EXPENSE_CATEGORIES', 'VIEW', 'View expense category list')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSE_CATEGORIES_CREATE', 'Create Expense Categories', 'EXPENSE_CATEGORIES', 'CREATE', 'Create new expense categories')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSE_CATEGORIES_EDIT', 'Edit Expense Categories', 'EXPENSE_CATEGORIES', 'EDIT', 'Modify expense category details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'EXPENSE_CATEGORIES_DELETE', 'Delete Expense Categories', 'EXPENSE_CATEGORIES', 'DELETE', 'Remove expense categories')
ON CONFLICT (permission_code) DO NOTHING;

-- INVENTORY permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVENTORY_VIEW', 'View Inventory', 'INVENTORY', 'VIEW', 'View inventory stock levels')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVENTORY_CREATE', 'Create Inventory', 'INVENTORY', 'CREATE', 'Add new inventory stock records')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVENTORY_EDIT', 'Edit Inventory', 'INVENTORY', 'EDIT', 'Modify inventory stock levels')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVENTORY_DELETE', 'Delete Inventory', 'INVENTORY', 'DELETE', 'Remove inventory stock records')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'INVENTORY_EXPORT', 'Export Inventory', 'INVENTORY', 'EXPORT', 'Export inventory data')
ON CONFLICT (permission_code) DO NOTHING;

-- CATEGORIES permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CATEGORIES_VIEW', 'View Categories', 'CATEGORIES', 'VIEW', 'View product category list')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CATEGORIES_CREATE', 'Create Categories', 'CATEGORIES', 'CREATE', 'Create new product categories')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CATEGORIES_EDIT', 'Edit Categories', 'CATEGORIES', 'EDIT', 'Modify product category details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CATEGORIES_DELETE', 'Delete Categories', 'CATEGORIES', 'DELETE', 'Remove product categories')
ON CONFLICT (permission_code) DO NOTHING;

-- SALES_ORDERS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SALES_ORDERS_VIEW', 'View Sales Orders', 'SALES_ORDERS', 'VIEW', 'View sales order list and details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SALES_ORDERS_CREATE', 'Create Sales Orders', 'SALES_ORDERS', 'CREATE', 'Create new sales orders')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SALES_ORDERS_EDIT', 'Edit Sales Orders', 'SALES_ORDERS', 'EDIT', 'Modify sales order details')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SALES_ORDERS_DELETE', 'Delete Sales Orders', 'SALES_ORDERS', 'DELETE', 'Remove sales orders')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SALES_ORDERS_EXPORT', 'Export Sales Orders', 'SALES_ORDERS', 'EXPORT', 'Export sales order data')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SALES_ORDERS_MANAGE', 'Manage Sales Orders', 'SALES_ORDERS', 'MANAGE', 'Complete, cancel and manage sales order lifecycle')
ON CONFLICT (permission_code) DO NOTHING;

-- CASH_REGISTER permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CASH_REGISTER_VIEW', 'View Cash Register', 'CASH_REGISTER', 'VIEW', 'View cash register sessions and history')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'CASH_REGISTER_MANAGE', 'Manage Cash Register', 'CASH_REGISTER', 'MANAGE', 'Open, close register and record cash movements')
ON CONFLICT (permission_code) DO NOTHING;

-- STOCK_ALERTS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'STOCK_ALERTS_VIEW', 'View Stock Alerts', 'STOCK_ALERTS', 'VIEW', 'View low stock and out of stock alerts')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'STOCK_ALERTS_MANAGE', 'Manage Stock Alerts', 'STOCK_ALERTS', 'MANAGE', 'Acknowledge and resolve stock alerts')
ON CONFLICT (permission_code) DO NOTHING;

-- RECEIPTS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'RECEIPTS_GENERATE', 'Generate Receipts', 'RECEIPTS', 'CREATE', 'Generate and print sales receipts')
ON CONFLICT (permission_code) DO NOTHING;

-- SALES_RETURNS permissions
INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SALES_RETURNS_VIEW', 'View Sales Returns', 'SALES_RETURNS', 'VIEW', 'View sales return history')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SALES_RETURNS_CREATE', 'Create Sales Returns', 'SALES_RETURNS', 'CREATE', 'Request a sales return')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO pos_core.permission (id, permission_code, permission_name, module, action, description)
VALUES (gen_random_uuid(), 'SALES_RETURNS_APPROVE', 'Approve Sales Returns', 'SALES_RETURNS', 'MANAGE', 'Approve or reject sales return requests')
ON CONFLICT (permission_code) DO NOTHING;
