package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqSupplierDTO {

    @NotBlank(message = "name must not be blank")
    private String name;

    @Pattern(regexp = "^$|^(0|\\+84)(3|5|7|8|9)\\d{8}$", message = "phone must be a valid Vietnam phone number")
    private String phone;

    private String address;

    @Email(message = "email must be valid")
    private String email;
}
