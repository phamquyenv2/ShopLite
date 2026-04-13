package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class ReqOfficeDTO {

    @NotBlank(message = "name must not be blank")
    private String name;

    @NotNull(message = "officeLat must not be null")
    private BigDecimal officeLat;

    @NotNull(message = "officeLng must not be null")
    private BigDecimal officeLng;

    @NotNull(message = "radius must not be null")
    @Positive(message = "radius must be positive")
    private Integer radius;

    private LocalTime shiftStart;

    private LocalTime shiftEnd;

    @Positive(message = "lateGraceMinutes must be positive")
    private Integer lateGraceMinutes;

    private LocalTime autoCheckoutTime;
}
