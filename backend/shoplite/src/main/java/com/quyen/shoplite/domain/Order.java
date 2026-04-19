package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.StatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** External / integration request ID */
    @Column(name = "request_id", length = 100, unique = true)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, unique = true, length = 50)
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
