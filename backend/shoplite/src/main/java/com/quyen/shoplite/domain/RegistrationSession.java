package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.RegSessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_sessions",
        indexes = @Index(name = "idx_reg_session_phone", columnList = "phone"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationSession {

    /** ID dạng "reg_" + UUID 8 ký tự, e.g. "reg_7f92c2a1" */
    @Id
    @Column(length = 50)
    private String id;

    /** Số điện thoại đã verify OTP (E.164) */
    @Column(nullable = false, length = 20)
    private String phone;

    /** Tên cửa hàng (sau bước 3) */
    @Column(name = "store_name", length = 200)
    private String storeName;

    /** OTP_VERIFIED → STORE_NAMED → COMPLETED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegSessionStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
