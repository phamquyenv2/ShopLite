package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.UserToken;
import com.quyen.shoplite.domain.response.ResLoginDTO;
import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.repository.UserTokenRepository;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy user: " + principalUsername));

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
        // Keep runtime validation consistent with ReqRegisterDTO
        if (!normalizedPhone.matches("^0\\d{9}$")) {
            throw new BadRequestException("số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0");
        }
        if (password.length() < 6) {
            throw new BadRequestException("mật khẩu phải có ít nhất 6 ký tự");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BadRequestException("Username '" + normalizedUsername + "' đã tồn tại");
        }
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new BadRequestException("Phone '" + normalizedPhone + "' đã tồn tại");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new BadRequestException("Role USER chưa được khởi tạo"));

        User user = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(password))
                .phone(normalizedPhone)
                .role(userRole)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        return issueTokens(saved);
    }

    public ResLoginDTO refresh(Jwt refreshJwt) {
        String tokenValue = refreshJwt.getTokenValue();
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không tồn tại hoặc đã bị thu hồi"));

        LocalDateTime now = LocalDateTime.now();
        if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(now)) {
            token.setRevoked(true);
            userTokenRepository.save(token);
            throw new UnauthorizedException("Refresh token đã hết hạn");
        }

        String username = refreshJwt.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy user: " + username));

        String roleName = (user.getRole() != null) ? user.getRole().getName() : "USER";

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

        return new ResLoginDTO(newAccessToken, newRefreshToken, userInfo);
    }

    public void logout(Jwt refreshJwt) {
        String tokenValue = refreshJwt.getTokenValue();
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không tồn tại hoặc đã bị thu hồi"));
        token.setRevoked(true);
        userTokenRepository.save(token);
    }

    private ResLoginDTO issueTokens(User user) {
        String roleName = (user.getRole() != null) ? user.getRole().getName() : "USER";

        String accessToken = securityUtil.generateAccessToken(user.getUsername(), roleName);
        String refreshToken = securityUtil.generateRefreshToken(user.getUsername());

        UserToken userToken = UserToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(securityUtil.getRefreshTokenExpiration()))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        userTokenRepository.save(userToken);

        ResLoginDTO.UserInfo userInfo = new ResLoginDTO.UserInfo(
                user.getId(), user.getUsername(), roleName);
        return new ResLoginDTO(accessToken, refreshToken, userInfo);
    }
}
