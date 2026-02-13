# Dynamic Authorization System

A complete Spring Boot application with **fully dynamic role-based access control (RBAC)**. Users can define roles and permissions at runtime without code changes.

## Features

✅ **100% Dynamic Authorization** - No hardcoded roles or permissions
✅ **Granular Permissions** - 6 permission flags: Create, Read, Update, Delete, Export, Approve
✅ **JWT Authentication** - Stateless authentication with refresh tokens
✅ **Database-Driven** - All roles and permissions stored in PostgreSQL
✅ **Method-Level Security** - Use `@PreAuthorize` with dynamic permission checks
✅ **User-Defined Roles** - Admins can create any role at runtime
✅ **User-Defined Permissions** - Admins can create permissions for any resource
✅ **Swagger UI** - Complete API documentation at `/swagger-ui.html`

## Architecture

### Core Components

1. **Entities**: `User`, `Role`, `Permission`, `RolePermission`
2. **Security**: `JwtService`, `JwtAuthenticationFilter`, `SecurityConfiguration`
3. **Dynamic Authorization**: `DynamicPermissionEvaluator` - The heart of dynamic permission checking
4. **Services**: `AuthenticationService`, `RoleService`, `PermissionService`
5. **Controllers**: `AuthenticationController`, `RoleController`, `PermissionController`

### How It Works

```
┌─────────────┐
│   Request   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────┐
│  JWT Filter extracts user       │
│  and loads authorities           │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  @PreAuthorize annotation        │
│  calls permissionEvaluator       │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  DynamicPermissionEvaluator      │
│  checks database for permission  │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  Access Granted or Denied        │
└─────────────────────────────────┘
```

## Quick Start

### Prerequisites

- Java 17+
- PostgreSQL 12+
- Maven 3.6+

### Setup

1. **Create Database**
```sql
CREATE DATABASE dynamic_auth_db;
```

2. **Update Configuration**

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/dynamic_auth_db
spring.datasource.username=your_username
spring.datasource.password=your_password
jwt.secret=your-secret-key-at-least-256-bits
```

3. **Run the Application**
```bash
mvn clean install
mvn spring-boot:run
```

4. **Access Swagger UI**

Open browser: `http://localhost:8080/swagger-ui.html`

## Usage Guide

### Step 1: Register a User

```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "john",
  "email": "john@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "usrId": "123e4567-e89b-12d3-a456-426614174000",
  "username": "john",
  "roles": ["USER"],
  "permissions": {}
}
```

### Step 2: Login

```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "john",
  "password": "password123"
}
```

Use the `accessToken` in subsequent requests:
```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Step 3: Create Permissions

Create permissions for the resources you want to protect:

```bash
POST /api/permissions
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "code": "PRODUCT_MANAGEMENT",
  "name": "Product Management",
  "description": "Manage products in the system",
  "resource": "products"
}
```

**More examples:**
- `USER_MANAGEMENT` - Manage users
- `ORDER_PROCESSING` - Process orders
- `INVENTORY_CONTROL` - Manage inventory
- `FINANCIAL_REPORTS` - Access financial reports

### Step 4: Create Roles

Create custom roles:

```bash
POST /api/roles
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "name": "PRODUCT_MANAGER",
  "description": "Manages products and inventory"
}
```

### Step 5: Assign Permissions to Roles

Grant granular permissions to a role:

```bash
POST /api/roles/{roleId}/permissions
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "permissionCode": "PRODUCT_MANAGEMENT",
  "canCreate": true,
  "canRead": true,
  "canUpdate": true,
  "canDelete": false,
  "canExport": true,
  "canApprove": false
}
```

This gives the role:
- ✅ Create products
- ✅ View products
- ✅ Update products
- ❌ Delete products (not allowed)
- ✅ Export products
- ❌ Approve products (not allowed)

## Using Dynamic Authorization in Controllers

### Example 1: Basic Permission Check

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    @PreAuthorize("@permissionEvaluator.canRead('PRODUCT_MANAGEMENT')")
    public List<Product> getAllProducts() {
        // Only users with PRODUCT_MANAGEMENT read permission can access
        return productService.findAll();
    }

    @PostMapping
    @PreAuthorize("@permissionEvaluator.canCreate('PRODUCT_MANAGEMENT')")
    public Product createProduct(@RequestBody Product product) {
        // Only users with PRODUCT_MANAGEMENT create permission can access
        return productService.save(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.canUpdate('PRODUCT_MANAGEMENT')")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product product) {
        // Only users with PRODUCT_MANAGEMENT update permission can access
        return productService.update(id, product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.canDelete('PRODUCT_MANAGEMENT')")
    public void deleteProduct(@PathVariable Long id) {
        // Only users with PRODUCT_MANAGEMENT delete permission can access
        productService.delete(id);
    }
}
```

### Example 2: Role-Based Check

```java
@GetMapping("/admin/dashboard")
@PreAuthorize("@permissionEvaluator.hasRole('ADMIN')")
public Dashboard getAdminDashboard() {
    // Only users with ADMIN role can access
    return dashboardService.getAdminDashboard();
}
```

### Example 3: Combining Permissions (OR)

```java
@GetMapping("/reports")
@PreAuthorize("@permissionEvaluator.canRead('FINANCIAL_REPORTS') or @permissionEvaluator.hasRole('ADMIN')")
public List<Report> getReports() {
    // Users with FINANCIAL_REPORTS read permission OR ADMIN role can access
    return reportService.getReports();
}
```

### Example 4: Combining Permissions (AND)

```java
@PostMapping("/products/{id}/publish")
@PreAuthorize("@permissionEvaluator.canUpdate('PRODUCT_MANAGEMENT') and @permissionEvaluator.canApprove('PRODUCT_MANAGEMENT')")
public Product publishProduct(@PathVariable Long id) {
    // User must have BOTH update AND approve permissions
    return productService.publish(id);
}
```

### Example 5: Multiple Roles

```java
@GetMapping("/analytics")
@PreAuthorize("@permissionEvaluator.hasAnyRole('ADMIN', 'MANAGER', 'ANALYST')")
public Analytics getAnalytics() {
    // Users with any of these roles can access
    return analyticsService.getAnalytics();
}
```

## Available Permission Evaluator Methods

```java
// Permission-based checks
@permissionEvaluator.canCreate('PERMISSION_CODE')
@permissionEvaluator.canRead('PERMISSION_CODE')
@permissionEvaluator.canUpdate('PERMISSION_CODE')
@permissionEvaluator.canDelete('PERMISSION_CODE')
@permissionEvaluator.canExport('PERMISSION_CODE')
@permissionEvaluator.canApprove('PERMISSION_CODE')

// Role-based checks
@permissionEvaluator.hasRole('ROLE_NAME')
@permissionEvaluator.hasAnyRole('ROLE1', 'ROLE2', 'ROLE3')
@permissionEvaluator.hasAllRoles('ROLE1', 'ROLE2')
```

## Common Use Cases

### 1. Multi-Tenant SaaS Application

Each tenant can define their own roles and permissions:
- Tenant A: "SALES_REP", "SALES_MANAGER", "SALES_DIRECTOR"
- Tenant B: "AGENT", "SUPERVISOR", "ADMIN"

### 2. E-Commerce Platform

Dynamic roles based on business needs:
- "PRODUCT_MANAGER" - Can manage products
- "ORDER_FULFILLMENT" - Can process orders
- "CUSTOMER_SUPPORT" - Can view customer data
- "FINANCIAL_ANALYST" - Can export financial reports

### 3. Healthcare System

HIPAA-compliant permission management:
- "DOCTOR" - Full patient record access
- "NURSE" - Read/update patient records
- "RECEPTIONIST" - Read-only access
- "BILLING" - Financial data only

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT tokens

### Role Management
- `GET /api/roles` - List all roles
- `POST /api/roles` - Create new role
- `GET /api/roles/{id}` - Get role details
- `DELETE /api/roles/{id}` - Delete role
- `POST /api/roles/{roleId}/permissions` - Assign permission to role
- `GET /api/roles/{roleId}/permissions` - Get role permissions
- `DELETE /api/roles/{roleId}/permissions/{permissionId}` - Remove permission from role

### Permission Management
- `GET /api/permissions` - List all permissions
- `POST /api/permissions` - Create new permission
- `GET /api/permissions/{id}` - Get permission details
- `DELETE /api/permissions/{id}` - Delete permission

### Example Endpoints (Demo)
- `GET /api/products` - List products (requires read permission)
- `POST /api/products` - Create product (requires create permission)
- `PUT /api/products/{id}` - Update product (requires update permission)
- `DELETE /api/products/{id}` - Delete product (requires delete permission)
- `GET /api/products/export` - Export products (requires export permission)
- `POST /api/products/{id}/approve` - Approve product (requires approve permission)

## Database Schema

The system uses 5 main tables:

1. **users** - User accounts
2. **roles** - User-defined roles
3. **permissions** - User-defined permissions
4. **user_roles** - Many-to-many: users ↔ roles
5. **role_permissions** - Many-to-many with granular flags: roles ↔ permissions

## Benefits of Dynamic Authorization

1. **No Code Changes** - Add new roles/permissions without deploying code
2. **Business Flexibility** - Adapt to changing business requirements instantly
3. **Multi-Tenant Ready** - Different tenants can have different permission structures
4. **Audit Trail** - All permission changes tracked in database
5. **Granular Control** - 6 different permission types per resource
6. **Self-Service** - Admins can manage permissions through API

## Security Best Practices

1. **JWT Secret** - Use a strong secret key (256+ bits)
2. **HTTPS Only** - Always use HTTPS in production
3. **Token Expiration** - Keep access token expiration short (2 hours)
4. **Refresh Tokens** - Use refresh tokens for long-lived sessions
5. **Password Hashing** - Uses BCrypt (automatically handled)
6. **SQL Injection** - Protected by JPA/Hibernate
7. **CORS** - Configure CORS properly for your frontend

## Troubleshooting

### Access Denied (403)

Check:
1. Is the user authenticated? (Valid JWT token)
2. Does the user have the required role?
3. Does the role have the required permission?
4. Is the permission code correct in `@PreAuthorize`?

### Token Expired

Use the refresh token to get a new access token:
```bash
POST /api/auth/refresh
Authorization: Bearer {refresh_token}
```

## License

MIT

## Contributing

Feel free to submit issues and pull requests!
# pos-system
