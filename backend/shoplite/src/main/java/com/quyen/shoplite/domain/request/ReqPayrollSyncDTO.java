package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReqPayrollSyncDTO {

    private Integer employeeId;

    @NotNull(message = "period is required")
    private LocalDate period;

    /** Thưởng chung cho kỳ lương này (mặc định 0) */
    private Double bonus = 0.0;

    /** Phạt thủ công cố định (mặc định 0) */
    private Double penalty = 0.0;

    /**
     * Số tiền phạt mỗi ngày nghỉ không phép hoặc bỏ ca không báo (mặc định 0).
     * Được nhân với số ngày vi phạm tự động tính từ Roster.
     */
    private Double penaltyPerAbsent = 0.0;
}

