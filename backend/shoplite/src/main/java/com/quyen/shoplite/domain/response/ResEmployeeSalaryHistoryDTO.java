package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.SalaryTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ResEmployeeSalaryHistoryDTO {
    private Integer id;
    private Integer employeeId;
    private String employeeUsername;
    private SalaryTypeEnum salaryType;
    private Double baseRate;
    private Double allowance;
    private Double commission;
    private Double recurringBonus;
    private Double recurringDeduction;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean current;
}
