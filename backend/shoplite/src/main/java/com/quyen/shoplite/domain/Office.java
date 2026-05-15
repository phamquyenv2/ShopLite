package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "offices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Office {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    /** Latitude of office location (GPS) */
    @Column(name = "office_lat", precision = 10, scale = 8)
    private BigDecimal officeLat;

    /** Longitude of office location (GPS) */
    @Column(name = "office_lng", precision = 11, scale = 8)
    private BigDecimal officeLng;

    /** Allowed radius in meters for check-in validation */
    @Column(nullable = false)
    @Builder.Default
    private Integer radius = 200;

    /** Store này thuộc về (FK — nullable để backward compat với data cũ) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @PrePersist
    void applyDefaultsForNewRow() {
        if (officeLat == null) officeLat = BigDecimal.ZERO;
        if (officeLng == null) officeLng = BigDecimal.ZERO;
    }
}
