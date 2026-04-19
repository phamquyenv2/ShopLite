package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResUserDTO {
    private Integer id;
    private Integer version;
    private String username;
    private String roleName;    // Tên role (thay cho RoleEnum)
    private Long roleId;
    private boolean isActive;
    private LocalDateTime createdAt;
}
