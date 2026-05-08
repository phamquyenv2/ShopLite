package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.RosterTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ResRosterDTO {
    private Integer id;

    // --- Employee info ---
    private Integer employeeId;
    private String employeeUsername;

    private LocalDate workingDay;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalTime checkInAllowedFrom;
    private LocalTime checkInAllowedTo;
    private LocalTime checkOutAllowedFrom;
    private LocalTime checkOutAllowedTo;
    private Double expectedHours;
    private RosterTypeEnum type;
    private String note;
    private Long unpaidBreakMinutes;
    private Boolean expired;
}
