package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.repository.UserTokenRepository;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.StoreMemberStatus;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.UnauthorizedException;

import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.UserToken;
import com.quyen.shoplite.domain.response.ResLoginDTO;
import com.quyen.shoplite.domain.response.ResMeDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final RoleRepository roleRepository;
    private final StoreMemberRepository storeMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MenuService menuService;

    public ResLoginDTO login(String username, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String principalUsername = authentication.getName();
        User user = userRepository.findByUsername(principalUsername)
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user: " + principalUsername));

        return issueTokens(user);
    }

    public ResLoginDTO register(String username, String phone, String password) {
        String normalizedUsername = (username == null) ? null : username.trim();
        String normalizedPhone = (phone == null) ? null : phone.trim();
        if (normalizedUsername == null || normalizedUsername.isBlank()) {
            throw new BadRequestException("username must not be blank");
        }
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            throw new BadRequestException("phone must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("password must not be blank");
        }
        if (!normalizedPhone.matches("^0\\d{9}$")) {
            throw new BadRequestException("so dien thoai phai gom 10 chu so va bat dau bang 0");
        }
        if (password.length() < 6) {
            throw new BadRequestException("mat khau phai co it nhat 6 ky tu");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BadRequestException("Username '" + normalizedUsername + "' da ton tai");
        }
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new BadRequestException("Phone '" + normalizedPhone + "' da ton tai");
        }

        User user = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(password))
                .phone(normalizedPhone)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        return issueTokens(saved);
    }

    public ResLoginDTO refresh(Jwt refreshJwt) {
        String tokenValue = refreshJwt.getTokenValue();
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new UnauthorizedException("Refresh token khong ton tai hoac da bi thu hoi"));

        LocalDateTime now = LocalDateTime.now();
        if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(now)) {
            token.setRevoked(true);
            userTokenRepository.save(token);
            throw new UnauthorizedException("Refresh token da het han");
        }

        String username = refreshJwt.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user: " + username));

        String roleName = getRoleName(user);

        String newAccessToken = securityUtil.generateAccessToken(user.getUsername(), roleName);
        String newRefreshToken = securityUtil.generateRefreshToken(user.getUsername());

        token.setRevoked(true);
        userTokenRepository.save(token);

        UserToken newUserToken = UserToken.builder()
                .user(user)
                .refreshToken(newRefreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(securityUtil.getRefreshTokenExpiration()))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        userTokenRepository.save(newUserToken);

        ResLoginDTO.UserInfo userInfo = new ResLoginDTO.UserInfo(
                user.getId(), user.getUsername(), roleName);
        ResLoginDTO response = new ResLoginDTO(newAccessToken, newRefreshToken, userInfo);
        response.setCurrentStore(buildCurrentStoreInfo(user));
        return response;
    }

    public void logout(Jwt refreshJwt) {
        String tokenValue = refreshJwt.getTokenValue();
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new UnauthorizedException("Refresh token khong ton tai hoac da bi thu hoi"));
        token.setRevoked(true);
        userTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public ResMeDTO getCurrentUserProfile(String jwtSubject) {
        User user = userRepository.findByUsername(jwtSubject)
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user: " + jwtSubject));

        List<StoreMember> memberships = storeMemberRepository
                .findAllByUserIdAndStatusFetchStore(user.getId(), StoreMemberStatus.ACTIVE);

        List<ResMeDTO.StoreInfo> stores = memberships.stream()
                .map(this::toMeStoreInfo)
                .toList();

        Long requestedStoreId = readStoreIdHeader();
        ResMeDTO.StoreInfo currentStore = null;
        if (!stores.isEmpty()) {
            currentStore = requestedStoreId == null
                    ? stores.get(0)
                    : stores.stream()
                            .filter(store -> store.getId().equals(requestedStoreId))
                            .findFirst()
                            .orElse(stores.get(0));
        }
        String globalRole = memberships.stream()
                .filter(m -> m.getStatus() == StoreMemberStatus.ACTIVE)
                .findFirst()
                .map(m -> m.getRole() != null ? m.getRole().getName() : null)
                .orElse(null);

        return ResMeDTO.builder()
                .user(ResMeDTO.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .phone(user.getPhone())
                        .globalRole(globalRole)
                        .build())
                .currentStore(currentStore)
                .stores(stores)
                .build();
    }

    private ResMeDTO.StoreInfo toMeStoreInfo(StoreMember sm) {
        Role role = sm.getRole();
        return ResMeDTO.StoreInfo.builder()
                .id(sm.getStore().getId())
                .name(sm.getStore().getName())
                .memberRole(role != null ? role.getName() : "USER")
                .membershipStatus(sm.getStatus().name())
                .permissions(role == null ? List.of() : role.getPermissions().stream()
                        .map(DTOMapper::toResPermissionDTO)
                        .toList())
                .menus(menuService.getVisibleMenus(role))
                .build();
    }

    private ResLoginDTO issueTokens(User user) {
        String roleName = getRoleName(user);
        LocalDateTime now = LocalDateTime.now();

        List<UserToken> validTokens = userTokenRepository.findValidTokensByUserId(user.getId(), now);

        String refreshToken;
        if (!validTokens.isEmpty()) {
            refreshToken = validTokens.get(0).getRefreshToken();
        } else {
            userTokenRepository.revokeAllByUserId(user.getId());

            refreshToken = securityUtil.generateRefreshToken(user.getUsername());
            UserToken newToken = UserToken.builder()
                    .user(user)
                    .refreshToken(refreshToken)
                    .expiresAt(now.plusSeconds(securityUtil.getRefreshTokenExpiration()))
                    .revoked(false)
                    .createdAt(now)
                    .build();
            userTokenRepository.save(newToken);
        }

        String accessToken = securityUtil.generateAccessToken(user.getUsername(), roleName);

        ResLoginDTO.UserInfo userInfo = new ResLoginDTO.UserInfo(
                user.getId(), user.getUsername(), roleName);
        ResLoginDTO response = new ResLoginDTO(accessToken, refreshToken, userInfo);
        response.setCurrentStore(buildCurrentStoreInfo(user));
        return response;
    }

    private String getRoleName(User user) {
        return storeMemberRepository
                .findAllByUserIdAndStatusFetchStore(user.getId(), StoreMemberStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(m -> m.getRole() != null ? m.getRole().getName() : "USER")
                .orElse("USER");
    }

    private ResLoginDTO.StoreInfo buildCurrentStoreInfo(User user) {
        return storeMemberRepository
                .findAllByUserIdAndStatusFetchStore(user.getId(), StoreMemberStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(sm -> new ResLoginDTO.StoreInfo(
                        sm.getStore().getId(),
                        sm.getStore().getName(),
                        sm.getRole() != null ? sm.getRole().getName() : "USER",
                        sm.getStatus().name(),
                        sm.getRole() == null ? List.of() : sm.getRole().getPermissions().stream()
                                .map(DTOMapper::toResPermissionDTO)
                                .toList(),
                        menuService.getVisibleMenus(sm.getRole())))
                .orElse(null);
    }

    private Long readStoreIdHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String rawStoreId = request.getHeader(CurrentStoreService.STORE_HEADER);
        if (rawStoreId == null || rawStoreId.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(rawStoreId.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
