package com.quyen.shoplite.domain.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqLoginDTO {
    @JsonAlias({"username"})
    @NotBlank(message = "phone must not be blank")
    private String phone;

    @NotBlank(message = "password must not be blank")
    private String password;
}
