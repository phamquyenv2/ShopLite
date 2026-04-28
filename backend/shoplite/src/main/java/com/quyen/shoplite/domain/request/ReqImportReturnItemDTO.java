package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqImportReturnItemDTO {

    @NotNull(message = "productId không được null")
    private Integer productId;

    @NotNull(message = "quantity không được null")
    @Min(value = 1, message = "quantity phải >= 1")
    private Integer quantity;

    @NotNull(message = "returnPrice không được null")
    @PositiveOrZero(message = "returnPrice phải >= 0")
    private Double returnPrice;
}
