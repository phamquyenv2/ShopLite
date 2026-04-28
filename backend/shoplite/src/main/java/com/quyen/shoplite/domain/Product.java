package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.quyen.shoplite.util.constant.ProductStatus;

@Entity
@Table(name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_store_sku", columnNames = {"store_id", "sku"}),
                @UniqueConstraint(name = "uk_products_store_barcode", columnNames = {"store_id", "barcode"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column
    private String barcode;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "cost_price", nullable = false)
    private Double costPrice;

    @Column(name = "selling_price", nullable = false)
    private Double sellingPrice;

    @Column(name = "min_stock")
    private Integer minStock;

    @Column(name = "max_stock")
    private Integer maxStock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    /**
     * Optimistic locking version
     */
    @Version
    private Integer version;

    @Column(length = 500)
    private String image;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void setStock(Integer stock) {
        this.stock = stock;
        if (this.stock != null) {
            if (this.stock <= 0) {
                if (this.status != ProductStatus.INACTIVE) {
                    this.status = ProductStatus.OUT_OF_STOCK;
                }
            } else {
                if (this.status == ProductStatus.OUT_OF_STOCK) {
                    this.status = ProductStatus.ACTIVE;
                }
            }
        }
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = ProductStatus.ACTIVE;
        }
        if (costPrice == null) {
            costPrice = 0d;
        }
        if (sellingPrice == null) {
            sellingPrice = 0d;
        }
        if (stock == null) {
            stock = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
