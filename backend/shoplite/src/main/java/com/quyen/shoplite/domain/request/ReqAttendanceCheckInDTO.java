package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqAttendanceCheckInDTO {

    @NotNull(message = "latitude khong duoc de trong")
    private Double latitude;

    @NotNull(message = "longitude khong duoc de trong")
    private Double longitude;

    /** Selected roster/shift id. Required for shift-based attendance. */
    private Integer rosterId;

    /** Optional client device identifier for audit and future anti-fraud checks */
    private String deviceId;
}
