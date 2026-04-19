package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers")
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

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    /** Loyalty / reward points */
    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;
}
