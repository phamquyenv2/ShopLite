package com.quyen.shoplite.config;

import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.repository.PermissionRepository;
import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final EmployeeRepository employeeRepository;
    private final OfficeRepository officeRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        jdbcTemplate.update("UPDATE roles SET version = 0 WHERE version IS NULL");
        jdbcTemplate.update("UPDATE users SET version = 0 WHERE version IS NULL");

        seedPermissionsAndRoles();
        seedAdminUser();
        seedEmployees();
    }

    private void seedPermissionsAndRoles() {
        List<Permission> desiredPermissions = List.of(
                // AUTH
                buildPermission("Login", "/api/v1/auth/login", "POST", "AUTH"),
                buildPermission("Confirm order", "/api/v1/orders/{id}/confirm", "PATCH", "ORDERS"),
                buildPermission("Register", "/api/v1/auth/register", "POST", "AUTH"),
                buildPermission("Refresh token", "/api/v1/auth/refresh", "POST", "AUTH"),
                buildPermission("Get current user", "/api/v1/auth/me", "GET", "AUTH"),
                buildPermission("Logout", "/api/v1/auth/logout", "POST", "AUTH"),
                // PRODUCTS
                buildPermission("Xem danh sách sản phẩm", "/api/v1/products", "GET", "PRODUCTS"),
                buildPermission("Xem sản phẩm theo ID", "/api/v1/products/{id}", "GET", "PRODUCTS"),
                buildPermission("Tạo sản phẩm", "/api/v1/products", "POST", "PRODUCTS"),
                buildPermission("Cập nhật sản phẩm", "/api/v1/products/{id}", "PUT", "PRODUCTS"),
                buildPermission("Xóa sản phẩm", "/api/v1/products/{id}", "DELETE", "PRODUCTS"),
                // CATEGORIES
                buildPermission("Xem danh mục", "/api/v1/categories", "GET", "CATEGORIES"),
                buildPermission("Xem danh mục theo ID", "/api/v1/categories/{id}", "GET", "CATEGORIES"),
                buildPermission("Tạo danh mục", "/api/v1/categories", "POST", "CATEGORIES"),
                buildPermission("Cập nhật danh mục", "/api/v1/categories/{id}", "PUT", "CATEGORIES"),
                buildPermission("Xóa danh mục", "/api/v1/categories/{id}", "DELETE", "CATEGORIES"),
                // CUSTOMERS
                buildPermission("Xem danh sách khách hàng", "/api/v1/customers", "GET", "CUSTOMERS"),
                buildPermission("Xác nhận đơn hàng", "/api/v1/orders/confirm", "PATCH", "ORDERS"),
                buildPermission("Xem khách hàng theo ID", "/api/v1/customers/{id}", "GET", "CUSTOMERS"),
                buildPermission("Tạo khách hàng", "/api/v1/customers", "POST", "CUSTOMERS"),
                buildPermission("Cập nhật khách hàng", "/api/v1/customers/{id}", "PUT", "CUSTOMERS"),
                buildPermission("Xóa khách hàng", "/api/v1/customers/{id}", "DELETE", "CUSTOMERS"),
                // SUPPLIERS
                buildPermission("Xem danh sách nhà cung cấp", "/api/v1/suppliers", "GET", "SUPPLIERS"),
                buildPermission("Xem nhà cung cấp theo ID", "/api/v1/suppliers/{id}", "GET", "SUPPLIERS"),
                buildPermission("Tạo nhà cung cấp", "/api/v1/suppliers", "POST", "SUPPLIERS"),
                buildPermission("Cập nhật nhà cung cấp", "/api/v1/suppliers/{id}", "PUT", "SUPPLIERS"),
                buildPermission("Xóa nhà cung cấp", "/api/v1/suppliers/{id}", "DELETE", "SUPPLIERS"),
                // UNITS
                buildPermission("Xem danh sách đơn vị", "/api/v1/units", "GET", "UNITS"),
                buildPermission("Xem đơn vị theo ID", "/api/v1/units/{id}", "GET", "UNITS"),
                buildPermission("Tạo đơn vị", "/api/v1/units", "POST", "UNITS"),
                buildPermission("Cập nhật đơn vị", "/api/v1/units/{id}", "PUT", "UNITS"),
                buildPermission("Xóa đơn vị", "/api/v1/units/{id}", "DELETE", "UNITS"),
                // ORDERS
                buildPermission("Xem danh sách đơn hàng", "/api/v1/orders", "GET", "ORDERS"),
                buildPermission("Xác nhận đơn hàng", "/api/v1/orders/confirm", "PATCH", "ORDERS"),
                buildPermission("Xem đơn hàng theo ID", "/api/v1/orders/{id}", "GET", "ORDERS"),
                buildPermission("Tạo đơn hàng", "/api/v1/orders", "POST", "ORDERS"),
                buildPermission("Cập nhật đơn hàng", "/api/v1/orders/{id}", "PUT", "ORDERS"),
                buildPermission("Cập nhật trạng thái ĐH", "/api/v1/orders/{id}/status", "PUT", "ORDERS"),
                buildPermission("Cập nhật trạng thái ĐH", "/api/v1/orders/{id}/status", "PATCH", "ORDERS"),
                buildPermission("Hủy đơn hàng", "/api/v1/orders/{id}", "DELETE", "ORDERS"),
                buildPermission("Tạo thanh toán cho đơn hàng", "/api/v1/orders/{id}/payments", "POST", "ORDERS"),
                buildPermission("Xem thanh toán theo đơn hàng", "/api/v1/orders/{id}/payments", "GET", "ORDERS"),
                // TRANSACTIONS
                buildPermission("Tạo giao dịch", "/api/v1/transactions", "POST", "TRANSACTIONS"),
                buildPermission("Xem danh sách giao dịch", "/api/v1/transactions", "GET", "TRANSACTIONS"),
                buildPermission("Xem giao dịch theo ID", "/api/v1/transactions/{id}", "GET", "TRANSACTIONS"),
                buildPermission("Xem giao dịch theo đơn hàng", "/api/v1/transactions/order/{orderId}", "GET", "TRANSACTIONS"),
                // PAYMENT
                buildPermission("Tạo phiên thanh toán", "/api/v1/payment/create", "POST", "PAYMENT"),
                // WEBHOOK
                buildPermission("Webhook SePay", "/api/webhook/sepay", "POST", "WEBHOOK"),
                // DEVICE TOKENS
                buildPermission("Đăng ký device token", "/api/v1/device-tokens/register", "POST", "DEVICE_TOKENS"),
                buildPermission("Xóa device token", "/api/v1/device-tokens", "DELETE", "DEVICE_TOKENS"),
                buildPermission("Test notification", "/api/v1/device-tokens/test-notification", "POST", "DEVICE_TOKENS"),
                // ATTENDANCE
                buildPermission("Chấm công check-in", "/api/v1/attendance/check-in", "POST", "ATTENDANCE"),
                buildPermission("Chấm công check-out", "/api/v1/attendance/check-out", "POST", "ATTENDANCE"),
                buildPermission("Xem chấm công hôm nay của tôi", "/api/v1/attendance/me/today", "GET", "ATTENDANCE"),
                buildPermission("Xem danh sách chấm công", "/api/v1/attendance", "GET", "ATTENDANCE"),
                buildPermission("Xem chấm công theo ID", "/api/v1/attendance/{id}", "GET", "ATTENDANCE"),
                // ROSTER
                buildPermission("Tạo lịch làm", "/api/v1/roster", "POST", "ROSTER"),
                buildPermission("Xem roster theo ID", "/api/v1/roster/{id}", "GET", "ROSTER"),
                buildPermission("Xem roster theo nhân viên", "/api/v1/roster/employee/{employeeId}", "GET", "ROSTER"),
                buildPermission("Xem roster theo ngày", "/api/v1/roster/day", "GET", "ROSTER"),
                buildPermission("Cập nhật roster", "/api/v1/roster/{id}", "PUT", "ROSTER"),
                buildPermission("Xóa roster", "/api/v1/roster/{id}", "DELETE", "ROSTER"),
                // PAYROLLS
                buildPermission("Sync payroll monthly", "/api/v1/payrolls/sync-monthly", "POST", "PAYROLLS"),
                buildPermission("Xem danh sách payrolls", "/api/v1/payrolls", "GET", "PAYROLLS"),
                buildPermission("Xem payroll theo ID", "/api/v1/payrolls/{id}", "GET", "PAYROLLS"),
                buildPermission("Xem payroll theo nhân viên", "/api/v1/payrolls/employee/{employeeId}", "GET", "PAYROLLS"),
                // EMPLOYEES
                buildPermission("Tạo employee", "/api/v1/employees", "POST", "EMPLOYEES"),
                buildPermission("Xem danh sách employees", "/api/v1/employees", "GET", "EMPLOYEES"),
                buildPermission("Xem employee theo ID", "/api/v1/employees/{id}", "GET", "EMPLOYEES"),
                buildPermission("Cập nhật employee", "/api/v1/employees/{id}", "PUT", "EMPLOYEES"),
                buildPermission("Xóa employee", "/api/v1/employees/{id}", "DELETE", "EMPLOYEES"),
                // OFFICES
                buildPermission("Tạo office", "/api/v1/offices", "POST", "OFFICES"),
                buildPermission("Xem danh sách offices", "/api/v1/offices", "GET", "OFFICES"),
                buildPermission("Xem office theo ID", "/api/v1/offices/{id}", "GET", "OFFICES"),
                buildPermission("Cập nhật office", "/api/v1/offices/{id}", "PUT", "OFFICES"),
                buildPermission("Xóa office", "/api/v1/offices/{id}", "DELETE", "OFFICES"),
                // IMPORT ORDERS
                buildPermission("Tạo import order", "/api/v1/import-orders", "POST", "IMPORT_ORDERS"),
                buildPermission("Xem danh sách import orders", "/api/v1/import-orders", "GET", "IMPORT_ORDERS"),
                buildPermission("Xem import order theo ID", "/api/v1/import-orders/{id}", "GET", "IMPORT_ORDERS"),
                buildPermission("Sửa import order", "/api/v1/import-orders/{id}", "PUT", "IMPORT_ORDERS"),
                buildPermission("Cập nhật trạng thái", "/api/v1/import-orders/{id}/status", "PUT", "IMPORT_ORDERS"),
                // INVENTORY
                buildPermission("Xem inventory log", "/api/v1/inventory-logs", "GET", "INVENTORY"),
                buildPermission("Tạo inventory log", "/api/v1/inventory-logs", "POST", "INVENTORY"),
                buildPermission("Xem inventory log theo sản phẩm", "/api/v1/inventory-logs/product/{productId}", "GET", "INVENTORY"),
                buildPermission("Tạo inventory adjustment", "/api/v1/inventory-adjustments", "POST", "INVENTORY"),
                buildPermission("Xem danh sách inventory adjustments", "/api/v1/inventory-adjustments", "GET", "INVENTORY"),
                buildPermission("Xem inventory adjustment theo ID", "/api/v1/inventory-adjustments/{id}", "GET", "INVENTORY"),
                // USERS
                buildPermission("Xem danh sách user", "/api/v1/users", "GET", "USERS"),
                buildPermission("Xem user theo ID", "/api/v1/users/{id}", "GET", "USERS"),
                buildPermission("Tạo user", "/api/v1/users", "POST", "USERS"),
                buildPermission("Cập nhật user", "/api/v1/users/{id}", "PUT", "USERS"),
                buildPermission("Xóa user", "/api/v1/users/{id}", "DELETE", "USERS"),
                // ROLES
                buildPermission("Xem danh sách role", "/api/v1/roles", "GET", "ROLES"),
                buildPermission("Xem role theo ID", "/api/v1/roles/{id}", "GET", "ROLES"),
                buildPermission("Tạo role", "/api/v1/roles", "POST", "ROLES"),
                buildPermission("Cập nhật role", "/api/v1/roles/{id}", "PUT", "ROLES"),
                buildPermission("Xóa role", "/api/v1/roles/{id}", "DELETE", "ROLES"),
                // PERMISSIONS
                buildPermission("Xem danh sách permission", "/api/v1/permissions", "GET", "PERMISSIONS"),
                buildPermission("Xem permission theo ID", "/api/v1/permissions/{id}", "GET", "PERMISSIONS"),
                buildPermission("Tạo permission", "/api/v1/permissions", "POST", "PERMISSIONS"),
                buildPermission("Cập nhật permission", "/api/v1/permissions/{id}", "PUT", "PERMISSIONS"),
                buildPermission("Xóa permission", "/api/v1/permissions/{id}", "DELETE", "PERMISSIONS")
        );

        List<Permission> allPermissions = new ArrayList<>();
        int createdCount = 0;
        for (Permission desired : desiredPermissions) {
            Permission persisted;
            var existing = permissionRepository.findByApiPathAndMethod(desired.getApiPath(), desired.getMethod());
            if (existing.isPresent()) {
                persisted = existing.get();
            } else {
                persisted = permissionRepository.save(desired);
                createdCount++;
            }
            allPermissions.add(persisted);
        }
        if (createdCount > 0) {
            log.info("Đã thêm mới {} permissions (tổng mong muốn: {})", createdCount, desiredPermissions.size());
        } else {
            log.info("Permissions đã đầy đủ (tổng mong muốn: {})", desiredPermissions.size());
        }

        // --- ORDER_STAFF: Nhân viên ghi đơn - tạo đơn, xem sản phẩm ---
        List<Permission> orderStaffPermissions = allPermissions.stream()
                .filter(p -> {
                    String path = p.getApiPath();
                    String method = p.getMethod();
                    // Auth permissions
                    if (path.startsWith("/api/v1/auth")) return true;
                    // View products & categories & units
                    if ("GET".equals(method) && (path.startsWith("/api/v1/products")
                            || path.startsWith("/api/v1/categories")
                            || path.startsWith("/api/v1/units"))) return true;
                    // View + create orders
                    if (path.startsWith("/api/v1/orders") && ("GET".equals(method) || "POST".equals(method))) return true;
                    // View customers
                    if ("GET".equals(method) && path.startsWith("/api/v1/customers")) return true;
                    // Attendance check-in/out
                    if (path.contains("/attendance/check-")) return true;
                    return false;
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        ensureRole("ORDER_STAFF", "Nhân viên ghi đơn - tạo đơn, xem sản phẩm", orderStaffPermissions);

        // --- CASHIER: Nhân viên thu ngân - thanh toán, quản lý quỹ ---
        List<Permission> cashierPermissions = allPermissions.stream()
                .filter(p -> {
                    String path = p.getApiPath();
                    String method = p.getMethod();
                    if (path.startsWith("/api/v1/auth")) return true;
                    // View products, categories, units
                    if ("GET".equals(method) && (path.startsWith("/api/v1/products")
                            || path.startsWith("/api/v1/categories")
                            || path.startsWith("/api/v1/units"))) return true;
                    // Orders: full access
                    if (path.startsWith("/api/v1/orders")) return true;
                    // Payment
                    if (path.startsWith("/api/v1/payment")) return true;
                    // Transactions
                    if (path.startsWith("/api/v1/transactions")) return true;
                    // Customers: view + create
                    if (path.startsWith("/api/v1/customers") && ("GET".equals(method) || "POST".equals(method))) return true;
                    // Attendance
                    if (path.contains("/attendance/check-")) return true;
                    return false;
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        ensureRole("CASHIER", "Nhân viên thu ngân - thanh toán, quản lý quỹ", cashierPermissions);

        // --- WAREHOUSE: Nhân viên kho - kiểm kho, nhập xuất hàng ---
        List<Permission> warehousePermissions = allPermissions.stream()
                .filter(p -> {
                    String path = p.getApiPath();
                    String method = p.getMethod();
                    if (path.startsWith("/api/v1/auth")) return true;
                    // Products: full CRUD
                    if (path.startsWith("/api/v1/products")) return true;
                    // Categories & Units: view
                    if ("GET".equals(method) && (path.startsWith("/api/v1/categories")
                            || path.startsWith("/api/v1/units"))) return true;
                    // Import orders: full
                    if (path.startsWith("/api/v1/import-orders")) return true;
                    // Inventory: full
                    if (path.startsWith("/api/v1/inventory")) return true;
                    // Suppliers: full
                    if (path.startsWith("/api/v1/suppliers")) return true;
                    // Attendance
                    if (path.contains("/attendance/check-")) return true;
                    return false;
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        ensureRole("WAREHOUSE", "Nhân viên kho - kiểm kho, nhập xuất hàng", warehousePermissions);

        // --- STORE_MANAGER: Quản lý cửa hàng - toàn quyền truy cập ---
        ensureRole("STORE_MANAGER", "Quản lý cửa hàng - toàn quyền truy cập", new ArrayList<>(allPermissions));
    }

    private void seedAdminUser() {
        if (userRepository.existsByUsername("1") || userRepository.existsByPhone("1")) {
            return;
        }

        Role managerRole = roleRepository.findByName("STORE_MANAGER").orElse(null);
        User admin = User.builder()
                .username("1")
                .phone("1")
                .password(passwordEncoder.encode("1"))
                .role(managerRole)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);
        log.info("Tạo tài khoản mặc định: username=1 / password=1 (role=STORE_MANAGER)");
    }

    private Permission buildPermission(String name, String apiPath, String method, String module) {
        return Permission.builder()
                .name(name)
                .apiPath(apiPath)
                .method(method)
                .module(module)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<Permission> mergePermissions(List<Permission> existing, List<Permission> toAdd) {
        Map<String, Permission> merged = new LinkedHashMap<>();
        if (existing != null) {
            for (Permission p : existing) {
                merged.put(p.getApiPath() + "#" + p.getMethod(), p);
            }
        }
        if (toAdd != null) {
            for (Permission p : toAdd) {
                merged.put(p.getApiPath() + "#" + p.getMethod(), p);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private void ensureRole(String name, String description, List<Permission> desiredPermissions) {
        Role role = roleRepository.findByName(name).orElse(null);
        if (role == null) {
            role = Role.builder()
                    .name(name)
                    .description(description)
                    .active(true)
                    .permissions(desiredPermissions)
                    .createdAt(LocalDateTime.now())
                    .version(0)
                    .build();
            roleRepository.save(role);
            log.info("Đã tạo role: {}", name);
        } else {
            List<Permission> merged = mergePermissions(role.getPermissions(), desiredPermissions);
            if (merged.size() != role.getPermissions().size()) {
                role.getPermissions().clear();
                role.getPermissions().addAll(merged);
                roleRepository.save(role);
                log.info("Đã cập nhật permissions cho role: {}", name);
            }
        }
    }

    private void seedEmployees() {
        // Skip if employees already exist
        if (employeeRepository.count() > 0) {
            return;
        }

        // 1. Ensure a default Office exists
        Office office = officeRepository.findAll().stream().findFirst().orElse(null);
        if (office == null) {
            office = Office.builder()
                    .name("Chi nhánh chính")
                    .officeLat(new BigDecimal("10.77620900"))
                    .officeLng(new BigDecimal("106.70076200"))
                    .radius(200)
                    .build();
            office = officeRepository.save(office);
            log.info("Đã tạo office mặc định: {}", office.getName());
        }

        // 2. Define sample employees: {username, phone, roleName}
        String[][] samples = {
                {"Nguyễn Văn A", "0901234567", "STORE_MANAGER"},
                {"Trần Thị B",   "0919876543", "ORDER_STAFF"},
                {"Lê Văn C",     "0987654321", "WAREHOUSE"},
                {"Phạm Thị D",   "0933334444", "CASHIER"},
                {"Hoàng Văn E",  "0971112222", "ORDER_STAFF"},
        };

        int created = 0;
        for (String[] s : samples) {
            String username = s[0];
            String phone = s[1];
            String roleName = s[2];

            if (userRepository.existsByUsername(username)) {
                continue;
            }

            Role role = roleRepository.findByName(roleName).orElse(null);

            User user = User.builder()
                    .username(username)
                    .phone(phone)
                    .password(passwordEncoder.encode("123456"))
                    .role(role)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);

            Employee employee = Employee.builder()
                    .user(user)
                    .office(office)
                    .salaryRate(0.0)
                    .build();
            employeeRepository.save(employee);
            created++;
        }

        if (created > 0) {
            log.info("Đã tạo {} nhân viên mẫu (password mặc định: 123456)", created);
        }
    }
}
