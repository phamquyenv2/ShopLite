package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.checkerframework.checker.units.qual.C;

@Entity
@Table(name = "user_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token", nullable = false, length = 512)
    private String refreshToken;

    @Column(name = "device_info", length = 300)
    private String deviceInfo;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked", columnDefinition = "TINYINT(1)", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
