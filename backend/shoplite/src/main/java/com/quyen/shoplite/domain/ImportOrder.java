package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.ImportOrderStatusEnum;
import com.quyen.shoplite.util.constant.ImportReturnStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column
    private Double tax;

    @Column
    private Double discount;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "amount_paid", nullable = false)
    @Builder.Default
    private Double amountPaid = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportOrderStatusEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status")
    @Builder.Default
    private ImportReturnStatusEnum returnStatus = ImportReturnStatusEnum.UNRETURNED;

    @Column(length = 1000)
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
