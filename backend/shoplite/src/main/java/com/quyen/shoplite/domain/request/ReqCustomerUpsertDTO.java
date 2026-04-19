package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqCustomerUpsertDTO {
    private Integer version;

    @NotBlank(message = "name must not be blank")
    @Size(max = 200, message = "name must be less than or equal to 200 characters")
    private String name;

    @NotBlank(message = "phone must not be blank")
    @Pattern(
            regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$",
            message = "phone must be a valid Vietnam phone number"
    )
    private String phone;
}
