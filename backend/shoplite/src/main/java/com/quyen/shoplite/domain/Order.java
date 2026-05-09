package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.StatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_store_code", columnNames = {"store_id", "code"}),
                @UniqueConstraint(name = "uk_orders_store_request_id", columnNames = {"store_id", "request_id"})
        },
        indexes = {
                @Index(name = "idx_orders_store_created", columnList = "store_id, created_at"),
                @Index(name = "idx_orders_store_status_created", columnList = "store_id, status, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Double discount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEnum status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
}
