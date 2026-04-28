package com.quyen.shoplite.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResOtpSendDTO {
    private String message;
    private String phone;
    private int expiresIn;
    private int resendAfter;
}
