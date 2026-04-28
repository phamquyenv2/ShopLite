package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_categories_store_name", columnNames = {"store_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 150)
    private String name;
}
