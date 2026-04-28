package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.PaymentStatusEnum;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_ref", columnList = "reference_type, reference_id"),
        @Index(name = "idx_payment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // ---- Polymorphic reference (thay thế @OneToOne Order) ----
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private RefTypeEnum referenceType;

    @Column(name = "reference_id", nullable = false)
    private Integer referenceId;

    // ---- Payment info ----
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethodEnum paymentMethod;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "transfer_content", length = 255)
    private String transferContent;

    @Column(name = "qr_url", length = 1000)
    private String qrUrl;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "raw_webhook_payload", columnDefinition = "TEXT")
    private String rawWebhookPayload;

    // ---- Status & time ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentStatusEnum status = PaymentStatusEnum.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    /** Cờ chống gửi push notification trùng — chỉ true sau khi FCM đã gửi thành công */
    @Column(name = "notification_sent", nullable = false)
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
