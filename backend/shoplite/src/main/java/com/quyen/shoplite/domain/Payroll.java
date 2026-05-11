package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.SalaryTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "payrolls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Pay period (e.g. 2024-04-01 = April 2024) */
    @Column(nullable = false)
    private LocalDate period;

    @Column(name = "salary_rate", nullable = false)
    private Double salaryRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", nullable = false, length = 30)
    @Builder.Default
    private SalaryTypeEnum salaryType = SalaryTypeEnum.HOURLY;

    @Column(nullable = false)
    @Builder.Default
    private Double allowance = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double commission = 0.0;

    @Column(name = "total_hours", nullable = false)
    private Double totalHours;

    @Column(nullable = false)
    @Builder.Default
    private Double bonus = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double penalty = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double deduction = 0.0;

    /**
     * Formula: total_salary = total_hours * salary_rate + bonus - penalty
     */
    @Column(name = "total_salary", nullable = false)
    private Double totalSalary;
}
