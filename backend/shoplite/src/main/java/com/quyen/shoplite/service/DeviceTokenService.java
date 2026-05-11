package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.DeviceTokenRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.error.IdInvalidException;

import com.quyen.shoplite.domain.DeviceToken;
import com.quyen.shoplite.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /**
     * Đăng ký hoặc cập nhật FCM token cho một user.
     * Nếu token đã tồn tại → cập nhật updatedAt.
     * Nếu chưa → tạo mới.
     */
    @Transactional
    public DeviceToken registerToken(Integer userId, String token, String deviceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy User id=" + userId));

        Optional<DeviceToken> existing = deviceTokenRepository.findByToken(token);
        if (existing.isPresent()) {
            DeviceToken dt = existing.get();
            dt.setUpdatedAt(LocalDateTime.now());
            dt.setDeviceType(deviceType);
            log.info("[DeviceToken] Updated existing token for user id={}", userId);
            return deviceTokenRepository.save(dt);
        }

        DeviceToken newToken = DeviceToken.builder()
                .user(user)
                .token(token)
                .deviceType(deviceType)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("[DeviceToken] Registered new token for user id={}", userId);
        return deviceTokenRepository.save(newToken);
    }

    /**
     * Xóa FCM token (khi logout hoặc token hết hạn).
     */
    @Transactional
    public void deleteToken(String token) {
        if (!deviceTokenRepository.existsByToken(token)) {
            log.warn("[DeviceToken] Token not found for deletion.");
            return;
        }
        deviceTokenRepository.deleteByToken(token);
        log.info("[DeviceToken] Token deleted successfully.");
    }

    /**
     * Lấy tất cả token của một user.
     */
    public List<DeviceToken> getTokensByUserId(Integer userId) {
        return deviceTokenRepository.findAllByUserId(userId);
    }
}
