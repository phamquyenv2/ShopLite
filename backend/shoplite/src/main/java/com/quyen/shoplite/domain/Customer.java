package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers",
        uniqueConstraints = @UniqueConstraint(name = "uk_customers_store_phone", columnNames = {"store_id", "phone"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    /** Loyalty / reward points */
    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;
}
