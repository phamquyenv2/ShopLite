package com.quyen.shoplite.config;

import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.service.CurrentStoreService;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.error.PermissionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;

/**
 * Interceptor kiểm tra quyền truy cập dựa trên Permission của Role.
 * Chạy sau Spring Security (JWT đã xác thực), trước khi vào Controller.
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final CurrentStoreService currentStoreService;

    @Override
    @Transactional
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String httpMethod = request.getMethod();

        // Lấy username từ JWT (SecurityContextHolder)
        String username = SecurityUtil.getCurrentUserLogin().orElse("");
        if (username.isEmpty()) {
            // Nếu chưa đăng nhập → Spring Security đã xử lý, bỏ qua ở đây
            return true;
        }

        StoreMember membership = currentStoreService.getCurrentStoreMembership();
        Role role = membership.getRole();

        if (role == null) {
            throw new PermissionException("Tài khoản chưa được gán Role");
        }

        List<Permission> permissions = role.getPermissions();

        boolean hasPermission = permissions.stream()
                .anyMatch(p -> p.getApiPath().equals(path) && p.getMethod().equalsIgnoreCase(httpMethod));

        if (!hasPermission) {
            System.out.println("⚠️ Mising Permission: [" + httpMethod + "] " + path + " for role: " + role.getName());
        }

        if (!hasPermission) {
            throw new PermissionException("You do not have permission to access this endpoint");
        }

        return true;
    }
}
