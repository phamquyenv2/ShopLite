package com.quyen.shoplite.domain.request;

import com.quyen.shoplite.util.constant.RosterTypeEnum;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ReqRosterDTO {

    
    @Positive(message = "employeeId must be greater than 0")
@NotNull(message = "employeeId không được để trống")
    private Integer employeeId;

    @NotNull(message = "workingDay không được để trống")
    private LocalDate workingDay;

    /** Giờ bắt đầu ca — bắt buộc khi type == WORKING */
    private LocalTime startTime;

    /** Giờ kết thúc ca — bắt buộc khi type == WORKING */
    private LocalTime endTime;

    private LocalTime checkInAllowedFrom;

    private LocalTime checkInAllowedTo;

    private LocalTime checkOutAllowedFrom;

    private LocalTime checkOutAllowedTo;

    @NotNull(message = "type không được để trống")
    private RosterTypeEnum type;

    private String note;

    /** Số phút nghỉ không lương trong ca */
    @PositiveOrZero(message = "unpaidBreakMinutes must be greater than or equal to 0")
private Long unpaidBreakMinutes = 0L;

}

