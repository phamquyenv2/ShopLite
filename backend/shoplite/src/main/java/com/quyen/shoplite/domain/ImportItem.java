package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "import_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id", nullable = false)
    private ImportOrder importOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "import_price", nullable = false)
    private Double importPrice;

    @Column(name = "sub_total", nullable = false)
    private Double subTotal;

    @Column(name = "returned_quantity")
    @Builder.Default
    private Integer returnedQuantity = 0;
}
