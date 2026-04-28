package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqOtpVerifyDTO {

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "OTP không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "OTP phải gồm 6 chữ số")
    private String otp;
}
