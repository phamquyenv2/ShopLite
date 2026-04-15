package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.UserToken;
import com.quyen.shoplite.domain.response.ResLoginDTO;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.repository.UserTokenRepository;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;

    /**
     * Xác thực username/password → sinh access token + refresh token.
     */
    public ResLoginDTO login(String username, String password) {
        // 1. Xác thực credentials
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Load user để lấy role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy user: " + username));

        String roleName = (user.getRole() != null) ? user.getRole().getName() : "USER";

        // 3. Sinh tokens
        String accessToken = securityUtil.generateAccessToken(user.getUsername(), roleName);
        String refreshToken = securityUtil.generateRefreshToken(user.getUsername());

        // 4. Lưu refresh token
        UserToken userToken = UserToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(securityUtil.getRefreshTokenExpiration()))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        userTokenRepository.save(userToken);

        // 5. Build response
        ResLoginDTO.UserInfo userInfo = new ResLoginDTO.UserInfo(
                user.getId(), user.getUsername(), roleName);

        return new ResLoginDTO(accessToken, refreshToken, userInfo);
    }

    /**
     * Làm mới access token bằng refresh token hợp lệ.
     */
    public ResLoginDTO refresh(Jwt refreshJwt) {
        String tokenValue = refreshJwt.getTokenValue();
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không tồn tại hoặc đã bị thu hồi"));

        String username = refreshJwt.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy user: " + username));

        String roleName = (user.getRole() != null) ? user.getRole().getName() : "USER";

        String newAccessToken = securityUtil.generateAccessToken(user.getUsername(), roleName);
        String newRefreshToken = securityUtil.generateRefreshToken(user.getUsername());

        // Thu hồi token cũ và tạo token mới
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

    /**
     * Thu hồi token khi logout.
     */
    public void logout(Jwt refreshJwt) {
        String tokenValue = refreshJwt.getTokenValue();
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không tồn tại hoặc đã bị thu hồi"));
        token.setRevoked(true);
        userTokenRepository.save(token);
    }
}
