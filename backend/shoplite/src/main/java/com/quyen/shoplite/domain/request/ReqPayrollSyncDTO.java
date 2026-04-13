package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReqPayrollSyncDTO {
    @Positive(message = "employeeId must be greater than 0")
private Integer employeeId;

    @NotNull(message = "period is required")
    private LocalDate period;

    /** Thưởng chung cho kỳ lương này (mặc định 0) */
    @Min(value = 0, message = "Bonus must be non-negative")
    private Double bonus = 0.0;

    /** Phạt thủ công cố định (mặc định 0) */
    @Min(value = 0, message = "Penalty must be non-negative")
    private Double penalty = 0.0;

    /**
     * Số tiền phạt mỗi ngày nghỉ không phép hoặc bỏ ca không báo (mặc định 0).
     * Được nhân với số ngày vi phạm tự động tính từ Roster.
     */
    @Min(value = 0, message = "PenaltyPerAbsent must be non-negative")
    private Double penaltyPerAbsent = 0.0;
}


