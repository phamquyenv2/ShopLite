package com.quyen.shoplite.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResOtpVerifyDTO {
    private String registerSessionId;
    private String phone;
    private int expiresIn;
}
