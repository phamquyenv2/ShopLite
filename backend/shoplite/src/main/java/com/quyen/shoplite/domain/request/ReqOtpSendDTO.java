package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqOtpSendDTO {

    @NotBlank(message = "So dien thoai khong duoc de trong")
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$",
             message = "So dien thoai phai la 10 so bat dau bang 0 hoac E.164")
    private String phone;

    @Size(max = 4096, message = "FCM token qua dai")
    private String fcmToken;
}
