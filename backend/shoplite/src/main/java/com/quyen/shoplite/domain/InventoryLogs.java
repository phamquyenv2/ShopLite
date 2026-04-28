package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_id")
    private InventoryAdjustment adjustment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_item_id")
    private ImportItem importItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItems orderItem;

    @Column(name = "quantity_in")
    private Integer quantityIn;

    @Column(name = "quantity_out")
    private Integer quantityOut;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeInventoryEnum type;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
