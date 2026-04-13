package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqImportItemDTO {

    @NotNull(message = "productId không được để trống")
    private Integer productId;

    @NotNull(message = "quantity không được để trống")
    @Min(value = 1, message = "quantity phải >= 1")
    private Integer quantity;

    @NotNull(message = "importPrice không được để trống")
    @PositiveOrZero(message = "importPrice phải >= 0")
    private Double importPrice;
}
