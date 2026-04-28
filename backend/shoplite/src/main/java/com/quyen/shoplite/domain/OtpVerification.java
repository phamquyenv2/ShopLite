package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications",
        indexes = {
                @Index(name = "idx_otp_phone_purpose", columnList = "phone, purpose"),
                @Index(name = "idx_otp_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Số điện thoại E.164 (e.g. +84912345678) */
    @Column(nullable = false, length = 20)
    private String phone;

    /** BCrypt hash của OTP 6 số */
    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    /** Mục đích: REGISTER | RESET_PASSWORD */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = 5;

    /** Số lần SMS đã gửi cho OTP này */
    @Column(name = "send_count", nullable = false)
    @Builder.Default
    private int sendCount = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
