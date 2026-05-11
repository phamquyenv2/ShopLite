package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.SalaryTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_salary_histories", indexes = {
        @Index(name = "idx_salary_history_store_employee", columnList = "store_id, employee_id"),
        @Index(name = "idx_salary_history_effective", columnList = "effective_from, effective_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSalaryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", nullable = false, length = 30)
    @Builder.Default
    private SalaryTypeEnum salaryType = SalaryTypeEnum.HOURLY;

    @Column(name = "base_rate", nullable = false)
    private Double baseRate;

    @Column(nullable = false)
    @Builder.Default
    private Double allowance = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double commission = 0.0;

    @Column(name = "recurring_bonus", nullable = false)
    @Builder.Default
    private Double recurringBonus = 0.0;

    @Column(name = "recurring_deduction", nullable = false)
    @Builder.Default
    private Double recurringDeduction = 0.0;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
