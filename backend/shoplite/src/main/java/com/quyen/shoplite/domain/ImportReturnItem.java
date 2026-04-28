package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "import_return_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_order_id", nullable = false)
    private ImportReturnOrder importReturnOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "return_price", nullable = false)
    private Double returnPrice;

    @Column(name = "sub_total", nullable = false)
    private Double subTotal;
}
