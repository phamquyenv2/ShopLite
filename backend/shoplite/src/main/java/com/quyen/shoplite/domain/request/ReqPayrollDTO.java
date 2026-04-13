package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReqPayrollDTO {

    
    @Positive(message = "employeeId must be greater than 0")
@NotNull(message = "employeeId không được để trống")
    private Integer employeeId;

    /**
     * Kỳ tính lương (chọn ngày đầu tháng, ví dụ 2024-04-01 = kỳ tháng 4/2024)
     */
    @NotNull(message = "period không được để trống")
    private LocalDate period;

    @NotNull(message = "salaryRate không được để trống")
    @Positive(message = "salaryRate phải lớn hơn 0")
    private Double salaryRate;

    @NotNull(message = "totalHours không được để trống")
    @Positive(message = "totalHours phải lớn hơn 0")
    private Double totalHours;

    /** Thưởng thêm (mặc định 0) */
    private Double bonus = 0.0;

    /** Trừ phạt (mặc định 0) */
    private Double penalty = 0.0;
}

