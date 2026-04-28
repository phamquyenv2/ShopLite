package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers",
        uniqueConstraints = @UniqueConstraint(name = "uk_suppliers_store_name", columnNames = {"store_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

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

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 200)
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
