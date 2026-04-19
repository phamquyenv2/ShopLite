package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ResRoleDTO {
    private Long id;
    private Integer version;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private List<ResPermissionDTO> permissions;
}
