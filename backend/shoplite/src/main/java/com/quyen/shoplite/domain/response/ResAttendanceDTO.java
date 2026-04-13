package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.AttendanceStatusEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ResAttendanceDTO {
    private Integer id;

    // --- Employee info ---
    private Integer employeeId;
    private String employeeUsername;

    // --- Office info ---
    private Integer officeId;
    private String officeName;

    // --- Roster info (null if walk-in) ---
    private Integer rosterId;

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    /** Số phút làm thực tế (checkOut - checkIn) */
    private Long workedMinutes;

    /**
     * Số phút được tính lương:
     *  - Có Roster: window của ca, trừ late, trừ early leave.
     *  - Walk-in: = workedMinutes.
     */
    private Long payableMinutes;

    private LocalDate workingDay;

    /** true nếu nhân viên không có trong lịch làm (walk-in shift) */
    private boolean walkIn;

    // --- GPS ---
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double distance;
    private BigDecimal checkOutLatitude;
    private BigDecimal checkOutLongitude;
    private Double checkOutDistance;

    /** Phút đi trễ (0 nếu đúng giờ hoặc trong grace period) */
    private Long lateMinutes;

    /** Phút về sớm (0 nếu check-out sau giờ kết thúc ca) */
    private Long earlyLeaveMinutes;

    private boolean closedAutomatically;

    private AttendanceStatusEnum status;
}

