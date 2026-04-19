package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.StatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodEnum method;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEnum status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
