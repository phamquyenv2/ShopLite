package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqInspectImportItemDTO {

    @NotNull(message = "importItemId khong duoc de trong")
    @Positive(message = "importItemId must be greater than 0")
    private Integer importItemId;

    @NotNull(message = "receivedQuantity khong duoc de trong")
    @Min(value = 0, message = "receivedQuantity must be greater than or equal to 0")
    private Integer receivedQuantity;

    private String note;
}
