package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.DirectionEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_fund_account", columnList = "fund_account_id"),
        @Index(name = "idx_txn_payment", columnList = "payment_id"),
        @Index(name = "idx_txn_time", columnList = "transaction_time"),
        @Index(name = "idx_txn_code", columnList = "transaction_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeTransactionEnum type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private DirectionEnum direction;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String content;

    // ---- FK relations ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_account_id", nullable = false)
    private FundAccount fundAccount;

    // ---- Balance snapshot ----
    @Column(name = "balance_before", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    // ---- Identifiers & time ----
    @Column(name = "transaction_code", nullable = false, unique = true, length = 50)
    private String transactionCode;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
