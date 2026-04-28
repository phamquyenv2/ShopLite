package com.quyen.shoplite.config;

import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Implement UserDetailsService để Spring Security load user từ database.
 * Dùng role.name làm authority thay vì RoleEnum.
 */
@Component
@RequiredArgsConstructor
public class UserDetailsCustom implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với phone: " + phone));

        if (!user.isActive()) {
            throw new IdInvalidException("Tài khoản '" + user.getUsername() + "' đã bị vô hiệu hóa");
        }

        // Dùng role.name từ Role entity; fallback là "USER" nếu chưa gán role
        String roleName = user.getStoreMemberships().stream()
                .filter(m -> m.getStatus() == com.quyen.shoplite.util.constant.StoreMemberStatus.ACTIVE)
                .findFirst()
                .map(m -> m.getRole() != null ? m.getRole().getName() : "USER")
                .orElse("USER");

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + roleName)))
                .build();
    }
}
