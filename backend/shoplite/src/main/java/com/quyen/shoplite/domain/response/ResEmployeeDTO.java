package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResEmployeeDTO {
    private Integer id;
    private Double salaryRate;
    private String qr;
    private String note;
    private boolean deleted;

    // --- User info ---
    private Integer userId;
    private String username;
    private String phone;
    private String roleName;

    // --- Office info ---
    private Integer officeId;
    private String officeName;
}

