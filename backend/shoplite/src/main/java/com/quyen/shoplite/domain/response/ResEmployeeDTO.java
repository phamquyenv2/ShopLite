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

    // --- User info ---
    private Integer userId;
    private String username;

    // --- Office info ---
    private Integer officeId;
    private String officeName;
}
