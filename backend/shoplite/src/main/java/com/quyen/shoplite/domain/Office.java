package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

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

    /** Planned shift start used for late calculation */
    @Column(name = "shift_start")
    @Builder.Default
    private LocalTime shiftStart = LocalTime.of(8, 0);

    /** Planned shift end used for early-leave calculation */
    @Column(name = "shift_end")
    @Builder.Default
    private LocalTime shiftEnd = LocalTime.of(17, 0);

    /** Grace window after shift start before counting lateness */
    @Column(name = "late_grace_minutes")
    @Builder.Default
    private Integer lateGraceMinutes = 0;

    /** Automatic close time for missing check-out on the same working day */
    @Column(name = "auto_checkout_time")
    @Builder.Default
    private LocalTime autoCheckoutTime = LocalTime.of(23, 59);

    @PrePersist
    void applyDefaultsForNewRow() {
        if (officeLat == null) officeLat = BigDecimal.ZERO;
        if (officeLng == null) officeLng = BigDecimal.ZERO;
    }
}
