package com.quyen.shoplite.service;

import com.google.firebase.messaging.*;
import com.quyen.shoplite.domain.DeviceToken;
import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final DeviceTokenRepository deviceTokenRepository;
    
    @org.springframework.beans.factory.annotation.Value("${shoplite.firebase.enabled:true}")
    private boolean firebaseEnabled;

    /**
     * Gửi push notification "Thanh toán thành công" tới tất cả device của user sở hữu order.
     */
    public void sendPaymentSuccessNotification(Order order) {
        User user = order.getUser();
        List<DeviceToken> deviceTokens = deviceTokenRepository.findAllByUser(user);

        if (deviceTokens.isEmpty()) {
            log.warn("[FCM] No device tokens found for user id={}. Notification skipped.", user.getId());
            return;
        }

        String title = "✅ Thanh toán thành công!";
        String body = String.format("Đơn hàng %s đã được thanh toán. Số tiền: %,.0f VNĐ.",
                order.getCode(), order.getTotalAmount());

        for (DeviceToken deviceToken : deviceTokens) {
            sendToToken(deviceToken.getToken(), title, body, order);
        }
    }

    public void sendPermissionsChangedNotification(User user, Long roleId, String roleName) {
        List<DeviceToken> deviceTokens = deviceTokenRepository.findAllByUser(user);

        if (deviceTokens.isEmpty()) {
            log.warn("[FCM] No device tokens found for user id={}. Permissions notification skipped.", user.getId());
            return;
        }

        String title = "Quyền truy cập đã thay đổi";
        String body = "ShopLite sẽ cập nhật lại quyền của tài khoản này.";

        for (DeviceToken deviceToken : deviceTokens) {
            sendPermissionsChangedToToken(deviceToken.getToken(), title, body, roleId, roleName);
        }
    }

    /**
     * Gửi notification tới một device token cụ thể.
     */
    public void sendToToken(String token, String title, String body, Order order) {
        if (!firebaseEnabled) {
            log.info("[MOCK FCM] Notification would have been sent to token {}... Title: '{}', Body: '{}'", 
                safeSubstring(token), title, body);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("order_code", order != null ? order.getCode() : "")
                    .putData("order_id", order != null ? String.valueOf(order.getId()) : "")
                    .putData("amount", order != null ? String.valueOf(order.getTotalAmount()) : "")
                    .putData("type", "PAYMENT_SUCCESS")
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("payment_channel")
                                    .setIcon("ic_notification")
                                    .setColor("#4CAF50")
                                    .setSound("default")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build())
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] Notification sent successfully. MessageId={}, Token={}...", messageId, safeSubstring(token));

        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Failed to send notification to token {}...: {}", safeSubstring(token), e.getMessage());

            // Nếu token không hợp lệ (unregistered), có thể xóa khỏi DB
            if (MessagingErrorCode.UNREGISTERED.equals(e.getMessagingErrorCode())
                    || MessagingErrorCode.INVALID_ARGUMENT.equals(e.getMessagingErrorCode())) {
                log.warn("[FCM] Token is invalid/unregistered. Removing from DB: {}...", safeSubstring(token));
                deviceTokenRepository.deleteByToken(token);
            }
        }
    }

    private void sendPermissionsChangedToToken(String token, String title, String body, Long roleId, String roleName) {
        if (!firebaseEnabled) {
            log.info("[MOCK FCM] Permissions notification would have been sent to token {}... Role: '{}'",
                    safeSubstring(token), roleName);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "PERMISSIONS_CHANGED")
                    .putData("role_id", roleId != null ? String.valueOf(roleId) : "")
                    .putData("role_name", roleName != null ? roleName : "")
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("permissions_channel")
                                    .setIcon("ic_notification")
                                    .setColor("#2563EB")
                                    .setSound("default")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .build())
                            .build())
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] Permissions notification sent. MessageId={}, Token={}...", messageId, safeSubstring(token));

        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Failed to send permissions notification to token {}...: {}", safeSubstring(token), e.getMessage());

            if (MessagingErrorCode.UNREGISTERED.equals(e.getMessagingErrorCode())
                    || MessagingErrorCode.INVALID_ARGUMENT.equals(e.getMessagingErrorCode())) {
                log.warn("[FCM] Token is invalid/unregistered. Removing from DB: {}...", safeSubstring(token));
                deviceTokenRepository.deleteByToken(token);
            }
        }
    }

    /**
     * Gửi notification thử nghiệm (không cần order).
     */
    public void sendTestNotification(String token, String title, String body) {
        if (!firebaseEnabled) {
            log.info("[MOCK FCM] Test notification would have been sent to token {}... Title: '{}', Body: '{}'", 
                safeSubstring(token), title, body);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "TEST")
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] Test notification sent. MessageId={}", messageId);

        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Failed to send test notification: {}", e.getMessage());
            throw new RuntimeException("FCM send failed: " + e.getMessage(), e);
        }
    }

    public void sendRegistrationOtpToToken(String token, String phone, String otp, int expiresInSeconds) {
        String title = "Ma xac thuc ShopLite";
        String body = "Ma OTP cua ban la " + otp;

        if (!firebaseEnabled) {
            log.info("[MOCK FCM-OTP] Phone={}, Token={}..., OTP={}", phone, safeSubstring(token), otp);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "REGISTER_OTP_CODE")
                    .putData("phone", phone)
                    .putData("otp", otp)
                    .putData("expires_in", String.valueOf(expiresInSeconds))
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("otp_channel")
                                    .setSound("default")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .build())
                            .build())
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM-OTP] OTP sent. MessageId={}, Phone={}, Token={}...", messageId, phone, safeSubstring(token));
        } catch (FirebaseMessagingException e) {
            log.error("[FCM-OTP] Failed to send OTP to token {}...: {}", safeSubstring(token), e.getMessage());
            throw new RuntimeException("FCM OTP send failed: " + e.getMessage(), e);
        }
    }

    private String safeSubstring(String s) {
        if (s == null) return "null";
        return s.length() > 20 ? s.substring(0, 20) : s;
    }
}
