package com.quyen.shoplite.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Gửi SMS qua Twilio.
 *
 * Nếu twilio.enabled=false, service sẽ LOG OTP ra console thay vì gửi SMS.
 * Hữu ích khi develop mà không có Twilio credentials thật.
 */
@Service
@Slf4j
public class TwilioService {
    private static final String VIRTUAL_PHONE_NUMBER = "+18777804236";

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-number}")
    private String fromNumber;

    @Value("${twilio.enabled:false}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (enabled) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio khởi tạo thành công. From: {}", fromNumber);
        } else {
            log.warn("Twilio DISABLED — OTP sẽ được log ra console (chỉ dùng khi dev/test)");
        }
    }

    public void sendOtp(String toPhone, String otp) {
        System.out.println("Sending OTP to: " + toPhone);
        System.out.println("Twilio TO (virtual): " + VIRTUAL_PHONE_NUMBER);
        String body = "Mã OTP của bạn là: " + otp
                + ". Mã có hiệu lực trong 1 phút. Không chia sẽ mã này với bất kì ai.";

        if (!enabled) {
            // ── Dev mode: chỉ log ──────────────────────────────────────────
            log.info("=== [DEV-OTP] {} → {} ===", toPhone, otp);
            return;
        }

        // ── Production: gửi qua Twilio ───────────────────────────────────
        try {
            Message.creator(
                    new PhoneNumber(VIRTUAL_PHONE_NUMBER),
                    new PhoneNumber(fromNumber),
                    body
            ).create();
            log.info("Đã gửi OTP SMS tới {}", toPhone);
        } catch (Exception e) {
            log.error("Lỗi gửi SMS Twilio tới {}: {}", toPhone, e.getMessage());
            throw new com.quyen.shoplite.util.error.BadRequestException("Không thể gửi SMS. Vui lòng thử lại sau.");
        }
    }
}
