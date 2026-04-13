package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Base salary rate (per hour or per day) */
    @Column(name = "salary_rate", nullable = false)
    private Double salaryRate;

    /** QR code string used for check-in */
    @Column(length = 300)
    private String qr;

    /** Optional note / remark for this employee record */
    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    private Office office;

    /** 1-1 or 1-n mapping to User */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Soft-delete flag — true khi nhân viên nghỉ luôn.
     * Các dữ liệu lịch sử (Order, Payroll...) vẫn giữ tham chiếu;
     * chỉ các read-queries trong EmployeeService lọc bỏ bản ghi này.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
