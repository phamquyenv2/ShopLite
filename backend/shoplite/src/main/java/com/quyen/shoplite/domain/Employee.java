package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employees_store_member", columnNames = {"store_member_id"}),
                @UniqueConstraint(name = "uk_employees_qr", columnNames = {"qr"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_member_id", nullable = false)
    private StoreMember storeMember;

    @Column(name = "salary_rate", nullable = false)
    private Double salaryRate;

    @Column(length = 300)
    private String qr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    private Office office;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(columnDefinition = "TEXT")
    private String note;
}
