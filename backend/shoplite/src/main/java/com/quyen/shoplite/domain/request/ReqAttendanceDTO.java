package com.quyen.shoplite.domain.request;

import com.quyen.shoplite.util.constant.AttendanceStatusEnum;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReqAttendanceDTO {

    
    @Positive(message = "employeeId must be greater than 0")
@NotNull(message = "employeeId không được để trống")
    private Integer employeeId;

    
    @Positive(message = "officeId must be greater than 0")
@NotNull(message = "officeId không được để trống")
    private Integer officeId;

    /** Thời gian check-in */
    private LocalDateTime checkIn;

    /** Thời gian check-out */
    private LocalDateTime checkOut;

    /** Ngày làm việc */
    @NotNull(message = "workingDay không được để trống")
    private LocalDate workingDay;

    /** Vĩ độ GPS tại thời điểm check-in */
    private BigDecimal latitude;

    /** Kinh độ GPS tại thời điểm check-in */
    private BigDecimal longitude;

    /** Khoảng cách đến văn phòng (meters) */
    private Double distance;

    /** Số phút đi trễ */
    private Long lateMinutes;

    private AttendanceStatusEnum status;
}

