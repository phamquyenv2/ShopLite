package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.SalaryTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ResPayrollDTO {
    private Integer id;

    // --- Employee info ---
    private Integer employeeId;
    private String employeeUsername;

    /** Kỳ lương (đầu tháng) */
    private LocalDate period;

    private Double salaryRate;
    private SalaryTypeEnum salaryType;
    private Double allowance;
    private Double commission;
    private Double totalHours;
    private Double bonus;
    private Double penalty;
    private Double deduction;

    /**
     * Tổng lương: totalHours * salaryRate + bonus - penalty
     */
    private Double totalSalary;

    // --- Breakdown từ Roster ---
    /** Số ngày WORKING trong lịch (có hoặc không có Attendance) */
    private int scheduledWorkingDays;
    /** Số ngày thực tế đi làm (có Attendance với hours > 0) */
    private int actualPresentDays;
    /** Số ngày nghỉ có phép (LEAVE_APPROVED) */
    private int approvedLeaveDays;
    /** Số ngày nghỉ không phép hoặc bỏ ca không báo (LEAVE_UNAPPROVED + bỏ ca) */
    private int absentWithoutLeaveDays;
}
