package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReqRoleDTO {
    private Integer version;

    @NotBlank(message = "name không được để trống")
    private String name;

    private String description;

    private boolean active = true;

    /** Danh sách permission IDs gán cho role này */
    private List<Long> permissionIds;
}
