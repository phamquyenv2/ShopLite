package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_return_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportReturnOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_order_id")
    private ImportOrder importOrder; // optional link to original import order

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column
    private Double discount;

    @Column(name = "amount_paid")
    @Builder.Default
    private Double amountPaid = 0.0; // NCC đã trả lại

    @Column(length = 1000)
    private String note;

    @Column(name = "created_by_username")
    private String createdByUsername;

    @Column(name = "received_by_username")
    private String receivedByUsername;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
