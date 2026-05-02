package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqStoreInvitationDTO {

    @NotBlank(message = "phone is required")
    private String phone;

    @NotNull(message = "roleId is required")
    @Positive(message = "roleId must be greater than 0")
    private Long roleId;
}
