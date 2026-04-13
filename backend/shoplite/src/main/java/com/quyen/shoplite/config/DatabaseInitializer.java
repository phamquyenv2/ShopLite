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
import java.util.List;

/**
 * Tự động seed dữ liệu khởi tạo:
 * - Role ADMIN với đầy đủ quyền
 * - Role USER với quyền đọc cơ bản
 * - Tài khoản admin mặc định
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
        if (roleRepository.count() > 0) return; // Đã seed rồi

        // --- Seed permissions cho từng module ---
        List<Permission> allPermissions = List.of(
                // PRODUCTS
                buildPermission("Xem danh sách sản phẩm",  "/api/v1/products",      "GET",    "PRODUCTS"),
                buildPermission("Xem sản phẩm theo ID",    "/api/v1/products/{id}", "GET",    "PRODUCTS"),
                buildPermission("Tạo sản phẩm",            "/api/v1/products",      "POST",   "PRODUCTS"),
                buildPermission("Cập nhật sản phẩm",       "/api/v1/products/{id}", "PUT",    "PRODUCTS"),
                buildPermission("Xóa sản phẩm",            "/api/v1/products/{id}", "DELETE", "PRODUCTS"),
                // CATEGORIES
                buildPermission("Xem danh mục",            "/api/v1/categories",      "GET",  "CATEGORIES"),
                buildPermission("Tạo danh mục",            "/api/v1/categories",      "POST", "CATEGORIES"),
                buildPermission("Cập nhật danh mục",       "/api/v1/categories/{id}", "PUT",  "CATEGORIES"),
                buildPermission("Xóa danh mục",            "/api/v1/categories/{id}", "DELETE","CATEGORIES"),
                // ORDERS
                buildPermission("Xem danh sách đơn hàng", "/api/v1/orders",         "GET",   "ORDERS"),
                buildPermission("Xem đơn hàng theo ID",   "/api/v1/orders/{id}",    "GET",   "ORDERS"),
                buildPermission("Tạo đơn hàng",           "/api/v1/orders",         "POST",  "ORDERS"),
                buildPermission("Cập nhật trạng thái ĐH", "/api/v1/orders/{id}/status","PUT","ORDERS"),
                // USERS
                buildPermission("Xem danh sách user",     "/api/v1/users",          "GET",   "USERS"),
                buildPermission("Xem user theo ID",       "/api/v1/users/{id}",     "GET",   "USERS"),
                buildPermission("Tạo user",               "/api/v1/users",          "POST",  "USERS"),
                buildPermission("Cập nhật user",          "/api/v1/users/{id}",     "PUT",   "USERS"),
                buildPermission("Xóa user",               "/api/v1/users/{id}",     "DELETE","USERS"),
                // ROLES
                buildPermission("Xem danh sách role",     "/api/v1/roles",          "GET",   "ROLES"),
                buildPermission("Xem role theo ID",       "/api/v1/roles/{id}",     "GET",   "ROLES"),
                buildPermission("Tạo role",               "/api/v1/roles",          "POST",  "ROLES"),
                buildPermission("Cập nhật role",          "/api/v1/roles/{id}",     "PUT",   "ROLES"),
                buildPermission("Xóa role",               "/api/v1/roles/{id}",     "DELETE","ROLES"),
                // PERMISSIONS
                buildPermission("Xem danh sách permission","/api/v1/permissions",    "GET",  "PERMISSIONS"),
                buildPermission("Tạo permission",          "/api/v1/permissions",    "POST", "PERMISSIONS"),
                buildPermission("Cập nhật permission",     "/api/v1/permissions/{id}","PUT", "PERMISSIONS"),
                buildPermission("Xóa permission",          "/api/v1/permissions/{id}","DELETE","PERMISSIONS"),
                // INVENTORY
                buildPermission("Xem inventory log",      "/api/v1/inventory-logs",  "GET",  "INVENTORY"),
                buildPermission("Tạo inventory log",      "/api/v1/inventory-logs",  "POST", "INVENTORY")
        );
        permissionRepository.saveAll(allPermissions);
        log.info("Đã seed {} permissions", allPermissions.size());

        // --- Role ADMIN: toàn quyền ---
        Role adminRole = Role.builder()
                .name("ADMIN")
                .description("Quản trị viên hệ thống - toàn quyền")
                .active(true)
                .permissions(allPermissions)
                .createdAt(LocalDateTime.now())
                .build();
        roleRepository.save(adminRole);

        // --- Role USER: chỉ xem ---
        List<Permission> userPermissions = allPermissions.stream()
                .filter(p -> p.getMethod().equals("GET"))
                .toList();
        Role userRole = Role.builder()
                .name("USER")
                .description("Người dùng thông thường - chỉ xem")
                .active(true)
                .permissions(userPermissions)
                .createdAt(LocalDateTime.now())
                .build();
        roleRepository.save(userRole);

        log.info("Đã seed roles: ADMIN, USER");
    }

    private void seedAdminUser() {
        if (userRepository.existsByUsername("admin")) return;

        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        User admin = User.builder()
                .username("admin")
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
}
