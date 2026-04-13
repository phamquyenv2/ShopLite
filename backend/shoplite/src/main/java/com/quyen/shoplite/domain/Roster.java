package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.RosterTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lịch làm việc (roster / ca làm) của từng nhân viên theo ngày.
 * Admin/Manager tạo trước lịch này; Payroll sẽ đối chiếu với Attendance
 * để tính lương chính xác theo từng loại ngày.
 */
@Entity
@Table(
        name = "rosters",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_roster_employee_day",
                        columnNames = {"employee_id", "working_day"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Ngày áp dụng lịch này */
    @Column(name = "working_day", nullable = false)
    private LocalDate workingDay;

    /** Giờ bắt đầu ca (null nếu type == OFF / LEAVE_*) */
    @Column(name = "start_time")
    private LocalTime startTime;

    /** Giờ kết thúc ca (null nếu type == OFF / LEAVE_*) */
    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * Số giờ dự kiến của ca — dùng để:
     *  • Tính lương ngày LEAVE_APPROVED.
     *  • Đối chiếu giờ thực tế của Attendance.
     */
    @Column(name = "expected_hours")
    @Builder.Default
    private Double expectedHours = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RosterTypeEnum type = RosterTypeEnum.WORKING;

    /**
     * Thời gian nghỉ không lương (nghỉ trưa, nghỉ giữa ca).
     * Sẽ bị trừ đi khi tính payableMinutes thực tế.
     */
    @Column(name = "unpaid_break_minutes")
    @Builder.Default
    private Long unpaidBreakMinutes = 0L;

    /** Ghi chú thêm cho ca này */
    @Column(columnDefinition = "TEXT")
    private String note;
}
