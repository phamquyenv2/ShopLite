package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqOtpSendDTO {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$",
             message = "Số điện thoại phải là 10 số bắt đầu bằng 0 hoặc E.164")
    private String phone;
}
