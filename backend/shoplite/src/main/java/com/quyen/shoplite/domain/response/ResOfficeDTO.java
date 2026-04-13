package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class ResOfficeDTO {
    private Integer id;
    private String name;
    private BigDecimal officeLat;
    private BigDecimal officeLng;
    private Integer radius;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private Integer lateGraceMinutes;
    private LocalTime autoCheckoutTime;
}
