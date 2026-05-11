package com.quyen.shoplite.domain.request;

import com.quyen.shoplite.util.constant.SalaryTypeEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReqEmployeeSalaryHistoryDTO {

    @NotNull(message = "salaryType is required")
    private SalaryTypeEnum salaryType;

    @NotNull(message = "baseRate is required")
    @Min(value = 0, message = "baseRate must be >= 0")
    private Double baseRate;

    @Min(value = 0, message = "allowance must be >= 0")
    private Double allowance = 0.0;

    @Min(value = 0, message = "commission must be >= 0")
    private Double commission = 0.0;

    @Min(value = 0, message = "recurringBonus must be >= 0")
    private Double recurringBonus = 0.0;

    @Min(value = 0, message = "recurringDeduction must be >= 0")
    private Double recurringDeduction = 0.0;

    private LocalDate effectiveFrom;

    private String reason;
}
