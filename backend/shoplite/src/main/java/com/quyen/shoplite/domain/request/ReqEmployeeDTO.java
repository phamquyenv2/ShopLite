package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqEmployeeDTO {

    @NotNull(message = "userId is required")
    private Integer userId;

    /** office_id is required (employee must belong to one office) */
    @NotNull(message = "officeId is required")
    private Integer officeId;

    /** salary_rate must be >= 0 */
    @NotNull(message = "salaryRate is required")
    @Min(value = 0, message = "salaryRate must be >= 0")
    private Double salaryRate;

    /** QR code for check-in (optional – auto-generated if null) */
    private String qr;

    private String note;
}
