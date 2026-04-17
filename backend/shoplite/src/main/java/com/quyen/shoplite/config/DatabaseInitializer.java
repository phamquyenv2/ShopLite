package com.quyen.shoplite.config;

import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.User;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tự động seed dữ liệu khởi tạo: - Role ADMIN với đầy đủ quyền - Role USER với
 * quyền đọc cơ bản - Tài khoản admin mặc định
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissionsAndRoles();
        seedAdminUser();
    }

    private void seedPermissionsAndRoles() {
        // --- Seed permissions cho từng module (idempotent: chỉ thêm mới, không xoá) ---
        List<Permission> desiredPermissions = List.of(
                // AUTH
                buildPermission("Login", "/api/v1/auth/login", "POST", "AUTH"),
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
                buildPermission("Xem đơn hàng theo ID", "/api/v1/orders/{id}", "GET", "ORDERS"),
                buildPermission("Tạo đơn hàng", "/api/v1/orders", "POST", "ORDERS"),
                // NOTE: controller đang dùng PATCH, giữ lại PUT permission cũ (nếu DB cũ đã có)
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
                buildPermission("Cập nhật trạng thái import order", "/api/v1/import-orders/{id}/status", "PUT", "IMPORT_ORDERS"),
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

        // --- Ensure ADMIN role has all permissions (chỉ thêm mới, không xoá) ---
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (adminRole == null) {
            adminRole = Role.builder()
                    .name("ADMIN")
                    .description("Quản trị viên hệ thống - toàn quyền")
                    .active(true)
                    .permissions(allPermissions)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            adminRole.setPermissions(mergePermissions(adminRole.getPermissions(), allPermissions));
        }
        roleRepository.save(adminRole);

        // --- Ensure USER role has at least all GET permissions (chỉ thêm mới, không xoá) ---
        List<Permission> desiredUserPermissions = allPermissions.stream()
                .filter(p -> "GET".equals(p.getMethod()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        Role userRole = roleRepository.findByName("USER").orElse(null);
        if (userRole == null) {
            userRole = Role.builder()
                    .name("USER")
                    .description("Người dùng thông thường - chỉ xem")
                    .active(true)
                    .permissions(desiredUserPermissions)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            userRole.setPermissions(mergePermissions(userRole.getPermissions(), desiredUserPermissions));
        }
        roleRepository.save(userRole);

        log.info("Đã đảm bảo roles: ADMIN, USER");
    }

    private void seedAdminUser() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        User admin = User.builder()
                .username("admin")
                .phone("0383870916")
                .password(passwordEncoder.encode("admin123"))
                .role(adminRole)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);
        log.info("Tạo tài khoản mặc định: username=admin / password=admin123");
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
}
