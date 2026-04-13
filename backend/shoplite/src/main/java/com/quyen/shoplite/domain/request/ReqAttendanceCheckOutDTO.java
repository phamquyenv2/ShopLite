package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqAttendanceCheckOutDTO {

    @NotNull(message = "latitude khong duoc de trong")
    private Double latitude;

    @NotNull(message = "longitude khong duoc de trong")
    private Double longitude;

    /** Optional client device identifier for audit and future anti-fraud checks */
    private String deviceId;
}
