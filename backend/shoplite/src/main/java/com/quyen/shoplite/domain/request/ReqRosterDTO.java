package com.quyen.shoplite.domain.request;

import com.quyen.shoplite.util.constant.RosterTypeEnum;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ReqRosterDTO {

    @NotNull(message = "employeeId không được để trống")
    private Integer employeeId;

    @NotNull(message = "workingDay không được để trống")
    private LocalDate workingDay;

    /** Giờ bắt đầu ca — bắt buộc khi type == WORKING */
    private LocalTime startTime;

    /** Giờ kết thúc ca — bắt buộc khi type == WORKING */
    private LocalTime endTime;

    @NotNull(message = "type không được để trống")
    private RosterTypeEnum type;

    private String note;

    /** Số phút nghỉ không lương trong ca */
    private Long unpaidBreakMinutes = 0L;

    @AssertTrue(message = "startTime và endTime bắt buộc khi type là WORKING")
    public boolean isTimesValidForWorkingType() {
        if (type == RosterTypeEnum.WORKING) {
            return startTime != null && endTime != null;
        }
        return true;
    }
}
