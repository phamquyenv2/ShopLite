package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqRegisterDTO {

    @NotBlank(message = "username không được để trống")
    private String username;

    @NotBlank(message = "số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
    private String phone;

    @NotBlank(message = "mật khẩu không được để trống")
    @Size(min = 6, message = "mật khẩu phải có ít nhất 6 ký tự")
    private String password;
}
