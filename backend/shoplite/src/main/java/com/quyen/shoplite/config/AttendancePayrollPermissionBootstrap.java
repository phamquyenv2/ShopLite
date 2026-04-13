package com.quyen.shoplite.config;

import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.repository.PermissionRepository;
import com.quyen.shoplite.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(10)
@RequiredArgsConstructor
public class AttendancePayrollPermissionBootstrap implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Permission> requiredPermissions = List.of(
                ensurePermission("Get permission detail", "/api/v1/permissions/{id}", "GET", "PERMISSIONS"),
                ensurePermission("Check in", "/api/v1/attendance/check-in", "POST", "ATTENDANCE"),
                ensurePermission("Check out", "/api/v1/attendance/check-out", "POST", "ATTENDANCE"),
                ensurePermission("Get my attendance today", "/api/v1/attendance/me/today", "GET", "ATTENDANCE"),
                ensurePermission("Get attendance list", "/api/v1/attendance", "GET", "ATTENDANCE"),
                ensurePermission("Get attendance detail", "/api/v1/attendance/{id}", "GET", "ATTENDANCE"),
                ensurePermission("Sync payroll monthly", "/api/v1/payrolls/sync-monthly", "POST", "PAYROLLS"),
                ensurePermission("Get payroll list", "/api/v1/payrolls", "GET", "PAYROLLS"),
                ensurePermission("Get employee payrolls", "/api/v1/payrolls/employee/{employeeId}", "GET", "PAYROLLS")
        );

        roleRepository.findByName("ADMIN").ifPresent(role -> mergePermissions(role, requiredPermissions));
        roleRepository.findByName("USER").ifPresent(role -> mergePermissions(role, requiredPermissions.stream()
                .filter(permission -> permission.getApiPath().startsWith("/api/v1/attendance"))
                .toList()));
    }

    private Permission ensurePermission(String name, String apiPath, String method, String module) {
        return permissionRepository.findByApiPathAndMethod(apiPath, method)
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .name(name)
                        .apiPath(apiPath)
                        .method(method)
                        .module(module)
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    private void mergePermissions(Role role, List<Permission> requiredPermissions) {
        List<Permission> merged = new ArrayList<>(role.getPermissions());
        for (Permission permission : requiredPermissions) {
            boolean exists = merged.stream().anyMatch(existing -> existing.getId().equals(permission.getId()));
            if (!exists) {
                merged.add(permission);
            }
        }
        role.setPermissions(merged);
        roleRepository.save(role);
    }
}
